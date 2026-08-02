package com.lauya.vselectronicwarfare.block.entity;

import com.lauya.vselectronicwarfare.VSElectronicWarfare;
import com.lauya.vselectronicwarfare.block.RomComputerBlock;
import com.lauya.vselectronicwarfare.menu.RomComputerMenu;
import com.lauya.vselectronicwarfare.registry.ModBlockEntities;
import com.lauya.vselectronicwarfare.registry.ModMenus;
import dan200.computercraft.api.filesystem.Mount;
import dan200.computercraft.api.filesystem.WritableMount;
import dan200.computercraft.core.filesystem.MemoryMount;
import dan200.computercraft.shared.computer.blocks.ComputerBlockEntity;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.config.Config;
import dan200.computercraft.shared.util.ComponentMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * A CC:Tweaked computer with a deliberately ephemeral ComputerCraft identity.
 * Only ROM configuration is saved, so copied block-entity NBT cannot duplicate
 * a normal computer's ID or filesystem.
 */
public final class RomComputerBlockEntity extends ComputerBlockEntity {
    private static final String NBT_PROGRAM = "RomProgram";
    private static final String NBT_STATUS = "RomStatus";
    private static final String NBT_ERROR = "RomError";
    private static final String ITEM_PROGRAM = "RomProgram";
    private static final String STATUS_FILE = ".vs_ew_rom_status";
    private static final String PROGRAM_FILE = ".vs_ew_rom_program";
    private static final Pattern PASTEBIN_ID = Pattern.compile("[A-Za-z0-9]{1,32}");

    private String pastebinId = "";
    private boolean activationConsumed;
    private boolean previousPowered;
    private String status = "waiting for redstone";
    private String error = "";
    private String observedStatus = "";
    private int statusPollTicks;

    public RomComputerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ROM_COMPUTER.get(), pos, state, ComputerFamily.NORMAL);
    }

    @Override
    protected ServerComputer createComputer(int id) {
        return new RomServerComputer((ServerLevel) getLevel(), getBlockPos(), id, label, getFamily(),
            Config.computerTermWidth, Config.computerTermHeight, ComponentMap.empty());
    }

    @Override
    protected void serverTick() {
        if (getLevel().isClientSide) return;

        boolean powered = getLevel().hasNeighborSignal(getBlockPos());
        if (!activationConsumed && powered && !previousPowered) {
            activationConsumed = true;
            startConfiguredProgram();
        }
        previousPowered = powered;

        super.serverTick();

        if (++statusPollTicks >= 10) {
            statusPollTicks = 0;
            pollLauncherStatus();
        }
    }

    public boolean setPastebinId(String value) {
        String normalized = normalizePastebinId(value);
        if (normalized == null) return false;

        pastebinId = normalized;
        error = "";
        status = pastebinId.isEmpty() ? "waiting for configuration" : "waiting for redstone";
        sync();
        return true;
    }

    public void startConfiguredProgram() {
        if (pastebinId.isEmpty()) {
            setFailure("No Pastebin ID is configured");
            return;
        }

        ServerComputer computer = createServerComputer();
        if (computer.isOn()) {
            status = "already running";
            sync();
            return;
        }

        try {
            WritableMount mount = computer.createRootMount();
            if (mount == null) {
                setFailure("Computer storage is unavailable");
                return;
            }

            if (mount.exists(STATUS_FILE)) mount.delete(STATUS_FILE);
            if (mount.exists(PROGRAM_FILE)) mount.delete(PROGRAM_FILE);
            writeFile(mount, "startup.lua", launcherFor(pastebinId));
            writeFile(mount, STATUS_FILE, "downloading");

            observedStatus = "downloading";
            error = "";
            status = "downloading";
            sync();
            computer.turnOn();
        } catch (IOException exception) {
            VSElectronicWarfare.LOGGER.warn("Unable to prepare ROM computer at {}", getBlockPos(), exception);
            setFailure("Unable to prepare computer storage");
        }
    }

    public void shutdownComputer() {
        ServerComputer computer = getServerComputer();
        if (computer != null) computer.shutdown();
        status = "stopped";
        error = "";
        sync();
    }

    public void reportInvalidProgram() {
        status = "error";
        error = "Enter a Pastebin ID or pastebin.com URL";
        playFailureBeep();
        sync();
    }

    public String getPastebinId() {
        return pastebinId;
    }

    public String getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public void copyConfigurationToItem(ItemStack stack) {
        if (!pastebinId.isEmpty()) stack.getOrCreateTag().putString(ITEM_PROGRAM, pastebinId);
    }

    public void readConfigurationFromItem(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(ITEM_PROGRAM)) setPastebinId(tag.getString(ITEM_PROGRAM));
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new RomComputerMenu(containerId, inventory, getBlockPos());
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.vs_electronic_warfare_fresh.rom_computer");
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        // Do not call the parent implementation: its ComputerId would be copied by schematics.
        // Runtime state deliberately stays transient, so every copied schematic is armed afresh.
        tag.putString(NBT_PROGRAM, pastebinId);
    }

    @Override
    protected void loadServer(CompoundTag tag) {
        pastebinId = normalizedOrEmpty(tag.getString(NBT_PROGRAM));
        activationConsumed = false;
        status = defaultStatus();
        error = "";
        observedStatus = "";
        previousPowered = false;
    }

    @Override
    protected void loadClient(CompoundTag tag) {
        pastebinId = normalizedOrEmpty(tag.getString(NBT_PROGRAM));
        activationConsumed = false;
        status = defaultStatus();
        error = "";
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putString(NBT_PROGRAM, pastebinId);
        tag.putString(NBT_STATUS, status);
        tag.putString(NBT_ERROR, error);
        return tag;
    }

    private void pollLauncherStatus() {
        ServerComputer computer = getServerComputer();
        if (computer == null) return;

        try {
            WritableMount mount = computer.createRootMount();
            if (mount == null || !mount.exists(STATUS_FILE)) return;
            String newStatus = readFile(mount, STATUS_FILE);
            if (newStatus.isBlank() || newStatus.equals(observedStatus)) return;

            observedStatus = newStatus;
            if (newStatus.equals("started")) {
                status = "running";
                error = "";
                playStartBeep();
            } else if (newStatus.startsWith("error:")) {
                setFailure(newStatus.substring("error:".length()));
                return;
            } else {
                status = newStatus;
            }
            sync();
        } catch (IOException exception) {
            VSElectronicWarfare.LOGGER.debug("Unable to read ROM computer status at {}", getBlockPos(), exception);
        }
    }

    private void setFailure(String message) {
        status = "error";
        error = message;
        observedStatus = "error:" + message;
        playFailureBeep();
        sync();
    }

    private void playStartBeep() {
        getLevel().playSound(null, getBlockPos(), SoundEvents.NOTE_BLOCK_HARP.value(), SoundSource.BLOCKS, 0.65F, 1.2F);
    }

    private void playFailureBeep() {
        Level level = getLevel();
        level.playSound(null, getBlockPos(), SoundEvents.NOTE_BLOCK_HARP.value(), SoundSource.BLOCKS, 0.65F, 0.6F);
        level.playSound(null, getBlockPos(), SoundEvents.NOTE_BLOCK_HARP.value(), SoundSource.BLOCKS, 0.65F, 0.85F);
    }

    private void sync() {
        setChanged();
        if (getLevel() != null) {
            getLevel().sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private String defaultStatus() {
        return pastebinId.isEmpty() ? "waiting for configuration" : "waiting for redstone";
    }

    private static void writeFile(WritableMount mount, String path, String content) throws IOException {
        try (var channel = mount.openForWrite(path)) {
            channel.write(ByteBuffer.wrap(content.getBytes(StandardCharsets.UTF_8)));
        }
    }

    private static String readFile(Mount mount, String path) throws IOException {
        int size = (int) Math.min(mount.getSize(path), 512);
        ByteBuffer buffer = ByteBuffer.allocate(size);
        try (var channel = mount.openForRead(path)) {
            channel.read(buffer);
        }
        return StandardCharsets.UTF_8.decode((ByteBuffer) buffer.flip()).toString().trim();
    }

    private static String launcherFor(String id) {
        return "local function status(value)\n"
            + "  local file = fs.open('" + STATUS_FILE + "', 'w')\n"
            + "  if file then file.write(value) file.close() end\n"
            + "end\n"
            + "status('downloading')\n"
            + "local downloaded = shell.run('pastebin', 'get', '" + id + "', '" + PROGRAM_FILE + "')\n"
            + "if not downloaded or not fs.exists('" + PROGRAM_FILE + "') then\n"
            + "  status('error:Pastebin download failed. Check CC HTTP settings and the ID.')\n"
            + "  return\n"
            + "end\n"
            + "status('started')\n"
            + "shell.run('" + PROGRAM_FILE + "')\n";
    }

    @Nullable
    public static String normalizePastebinId(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) return "";
        normalized = normalized.replace("https://", "").replace("http://", "");
        if (normalized.toLowerCase(Locale.ROOT).startsWith("pastebin.com/")) {
            normalized = normalized.substring("pastebin.com/".length());
            if (normalized.startsWith("raw/")) normalized = normalized.substring("raw/".length());
        }
        return PASTEBIN_ID.matcher(normalized).matches() ? normalized : null;
    }

    private static String normalizedOrEmpty(String value) {
        String normalized = normalizePastebinId(value);
        return normalized == null ? "" : normalized;
    }

    private static final class RomServerComputer extends ServerComputer {
        private final WritableMount rootMount = new MemoryMount(Config.computerSpaceLimit);

        private RomServerComputer(ServerLevel level, BlockPos position, int computerId, @Nullable String label,
                                  ComputerFamily family, int terminalWidth, int terminalHeight, ComponentMap components) {
            super(level, position, computerId, label, family, terminalWidth, terminalHeight, components);
        }

        @Override
        public WritableMount createRootMount() {
            return rootMount;
        }
    }
}
