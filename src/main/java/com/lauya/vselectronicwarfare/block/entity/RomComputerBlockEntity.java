package com.lauya.vselectronicwarfare.block.entity;

import com.lauya.vselectronicwarfare.VSElectronicWarfare;
import com.lauya.vselectronicwarfare.block.RomComputerBlock;
import com.lauya.vselectronicwarfare.menu.RomComputerMenu;
import com.lauya.vselectronicwarfare.network.ModNetwork;
import com.lauya.vselectronicwarfare.network.RomComputerTerminalInputPacket;
import com.lauya.vselectronicwarfare.network.RomComputerTerminalPacket;
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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * A CC:Tweaked computer with a deliberately ephemeral ComputerCraft identity.
 * Only ROM configuration is saved, so copied block-entity NBT cannot duplicate
 * a normal computer's ID or filesystem.
 */
public class RomComputerBlockEntity extends ComputerBlockEntity {
    public static final int MAX_SCRIPT_BYTES = 16 * 1024;
    private static final String NBT_SCRIPT = "RomScript";
    private static final String LEGACY_NBT_PROGRAM = "RomProgram";
    private static final String NBT_STATUS = "RomStatus";
    private static final String NBT_ERROR = "RomError";
    private static final String ITEM_SCRIPT = "RomScript";
    private static final String LEGACY_ITEM_PROGRAM = "RomProgram";
    private static final String STATUS_FILE = ".vs_ew_rom_status";
    private static final String SCRIPT_FILE = ".vs_ew_rom_script";

    private String script = "";
    private boolean activationConsumed;
    private boolean previousPowered;
    private String status = "waiting for redstone";
    private String error = "";
    private String observedStatus = "";
    private int statusPollTicks;
    private int terminalSyncTicks;

    public RomComputerBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.ROM_COMPUTER.get(), pos, state, ComputerFamily.NORMAL);
    }

    protected RomComputerBlockEntity(BlockEntityType<? extends ComputerBlockEntity> type, BlockPos pos, BlockState state,
                                     ComputerFamily family) {
        super(type, pos, state, family);
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
        if (++terminalSyncTicks >= 2) {
            terminalSyncTicks = 0;
            syncTerminal();
        }
    }

    public boolean setScript(String value) {
        String normalized = normalizeScript(value);
        if (!isValidScript(normalized)) return false;

        script = normalized;
        error = "";
        status = script.isBlank() ? "waiting for configuration" : "waiting for redstone";
        sync();
        return true;
    }

    public void startConfiguredProgram() {
        if (script.isBlank()) {
            setFailure("No startup script is configured");
            return;
        }

        // A newly started server computer gets a clean memory filesystem.
        if (getServerComputer() != null) unload();
        ServerComputer computer = createServerComputer();

        try {
            WritableMount mount = computer.createRootMount();
            if (mount == null) {
                setFailure("Computer storage is unavailable");
                return;
            }

            clearMount(mount);
            writeFile(mount, "startup.lua", launcher());
            writeFile(mount, SCRIPT_FILE, script);
            writeFile(mount, STATUS_FILE, "ready");

            observedStatus = "ready";
            error = "";
            status = "starting";
            activationConsumed = true;
            sync();
            playStartBeep();
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

    public void reportInvalidScript() {
        status = "error";
        error = "Startup script exceeds 16 KiB";
        playFailureBeep();
        sync();
    }

    public String getScript() {
        return script;
    }

    public String getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public boolean isOperatorOnly() {
        return false;
    }

    public boolean canUse(Player player) {
        return !isOperatorOnly() || player.hasPermissions(2);
    }

    public void handleTerminalInput(RomComputerTerminalInputPacket packet) {
        ServerComputer computer = getServerComputer();
        if (computer == null || !isTerminalInteractive(computer)) return;

        switch (packet.action()) {
            case CHAR -> computer.queueEvent("char", new Object[]{packet.text()});
            case PASTE -> computer.queueEvent("paste", new Object[]{packet.text()});
            case KEY_DOWN -> computer.keyDown(packet.key(), packet.repeat());
            case KEY_UP -> computer.keyUp(packet.key());
            case MOUSE_CLICK -> computer.mouseClick(packet.key(), packet.x(), packet.y());
            case MOUSE_UP -> computer.mouseUp(packet.key(), packet.x(), packet.y());
            case MOUSE_DRAG -> computer.mouseDrag(packet.key(), packet.x(), packet.y());
            case MOUSE_SCROLL -> computer.mouseScroll(packet.key(), packet.x(), packet.y());
        }
    }

    public void copyConfigurationToItem(ItemStack stack) {
        if (!script.isBlank()) stack.getOrCreateTag().putString(ITEM_SCRIPT, script);
    }

    public void readConfigurationFromItem(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return;
        if (tag.contains(ITEM_SCRIPT)) {
            setScript(tag.getString(ITEM_SCRIPT));
        } else if (tag.contains(LEGACY_ITEM_PROGRAM)) {
            setScript(migrateLegacyProgram(tag.getString(LEGACY_ITEM_PROGRAM)));
        }
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new RomComputerMenu(containerId, inventory, getBlockPos());
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getDisplayNameKey());
    }

    protected String getDisplayNameKey() {
        return "block.vs_electronic_warfare.rom_computer";
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        // Do not call the parent implementation: its ComputerId would be copied by schematics.
        // Runtime state deliberately stays transient, so every copied schematic starts unarmed.
        tag.putString(NBT_SCRIPT, script);
    }

    @Override
    protected void loadServer(CompoundTag tag) {
        script = loadScript(tag);
        activationConsumed = false;
        status = defaultStatus();
        error = "";
        observedStatus = "";
        previousPowered = false;
    }

    @Override
    protected void loadClient(CompoundTag tag) {
        script = loadScript(tag);
        activationConsumed = false;
        status = tag.contains(NBT_STATUS) ? tag.getString(NBT_STATUS) : defaultStatus();
        error = tag.getString(NBT_ERROR);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putString(NBT_SCRIPT, script);
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
            if (newStatus.startsWith("running:")) {
                status = "running line " + newStatus.substring("running:".length());
                error = "";
            } else if (newStatus.equals("completed")) {
                status = "completed";
                error = "";
            } else if (newStatus.startsWith("error:")) {
                setFailure(formatScriptError(newStatus));
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
            getLevel().sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_INVISIBLE);
        }
    }

    private void syncTerminal() {
        ServerComputer computer = getServerComputer();
        if (computer == null) return;

        ModNetwork.CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
            getBlockPos().getX() + 0.5D, getBlockPos().getY() + 0.5D, getBlockPos().getZ() + 0.5D, 8.0D, getLevel().dimension()
        )), new RomComputerTerminalPacket(getBlockPos(), isTerminalInteractive(computer), computer.getTerminalState()));
    }

    private boolean isTerminalInteractive(ServerComputer computer) {
        return computer.isOn() && !status.equals("completed") && !status.equals("error") && !status.equals("stopped");
    }

    private String defaultStatus() {
        return script.isBlank() ? "waiting for configuration" : "waiting for redstone";
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

    private static void clearMount(WritableMount mount) throws IOException {
        List<String> entries = new ArrayList<>();
        mount.list("", entries);
        for (String entry : entries) mount.delete(entry);
    }

    private static String launcher() {
        return "local function status(value)\n"
            + "  local file = fs.open('" + STATUS_FILE + "', 'w')\n"
            + "  if file then file.write(value) file.close() end\n"
            + "end\n"
            + "local script, open_error = fs.open('" + SCRIPT_FILE + "', 'r')\n"
            + "if not script then status('error:0:Cannot open startup script') return end\n"
            + "local line_number = 0\n"
            + "while true do\n"
            + "  local line = script.readLine()\n"
            + "  if not line then break end\n"
            + "  line_number = line_number + 1\n"
            + "  local command = line:match('^%s*(.-)%s*$')\n"
            + "  if command ~= '' and command:sub(1, 1) ~= '#' then\n"
            + "    status('running:' .. line_number)\n"
            + "    if not shell.run(command) then\n"
            + "      status('error:' .. line_number .. ':' .. command:sub(1, 180))\n"
            + "      script.close()\n"
            + "      return\n"
            + "    end\n"
            + "  end\n"
            + "end\n"
            + "script.close()\n"
            + "status('completed')\n";
    }

    private static boolean isValidScript(@Nullable String value) {
        return value == null || value.getBytes(StandardCharsets.UTF_8).length <= MAX_SCRIPT_BYTES;
    }

    private static String loadScript(CompoundTag tag) {
        String loaded = tag.contains(NBT_SCRIPT)
            ? tag.getString(NBT_SCRIPT)
            : migrateLegacyProgram(tag.getString(LEGACY_NBT_PROGRAM));
        String normalized = normalizeScript(loaded);
        return isValidScript(normalized) ? normalized : "";
    }

    private static String normalizeScript(@Nullable String value) {
        return value == null ? "" : value.replace("\r\n", "\n").replace('\r', '\n');
    }

    private static String migrateLegacyProgram(String programId) {
        return programId == null || programId.isBlank() ? "" : "pastebin run " + programId.trim();
    }

    private static String formatScriptError(String statusValue) {
        String[] parts = statusValue.split(":", 3);
        if (parts.length < 3) return "Startup command failed";
        return "Line " + parts[1] + " failed: " + parts[2];
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
