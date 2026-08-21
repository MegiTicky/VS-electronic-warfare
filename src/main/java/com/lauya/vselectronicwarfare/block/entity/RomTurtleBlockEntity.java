package com.lauya.vselectronicwarfare.block.entity;

import com.lauya.vselectronicwarfare.network.ModNetwork;
import com.lauya.vselectronicwarfare.network.RomComputerTerminalInputPacket;
import com.lauya.vselectronicwarfare.network.RomComputerTerminalPacket;
import com.lauya.vselectronicwarfare.registry.ModBlockEntities;
import com.lauya.vselectronicwarfare.registry.ModMenus;
import dan200.computercraft.api.filesystem.Mount;
import dan200.computercraft.api.filesystem.WritableMount;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.api.upgrades.UpgradeData;
import dan200.computercraft.core.filesystem.MemoryMount;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.config.Config;
import dan200.computercraft.shared.turtle.blocks.TurtleBlockEntity;
import dan200.computercraft.shared.turtle.upgrades.TurtleModem;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class RomTurtleBlockEntity extends TurtleBlockEntity implements RomComputerAccess {
    private static final String NBT_SCRIPT = "RomScript";
    private static final String NBT_STATUS = "RomStatus";
    private static final String NBT_ERROR = "RomError";
    private static final String ITEM_SCRIPT = "RomScript";
    private static final String STATUS_FILE = ".vs_ew_rom_status";
    private static final String SCRIPT_FILE = ".vs_ew_rom_script";

    private final boolean advancedModem;

    private String script = "";
    private boolean activationConsumed;
    private boolean previousPowered;
    private String status = "waiting for redstone";
    private String error = "";
    private String observedStatus = "";
    private int statusPollTicks;
    private int terminalSyncTicks;

    public RomTurtleBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.ROM_TURTLE.get(), pos, state, ComputerFamily.NORMAL, false);
    }

    protected RomTurtleBlockEntity(BlockEntityType<? extends TurtleBlockEntity> type, BlockPos pos, BlockState state,
                                   ComputerFamily family, boolean advancedModem) {
        super(type, pos, state, () -> 20_000, family);
        this.advancedModem = advancedModem;
    }

    @Override
    protected void serverTick() {
        ensureModem();
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

    public void ensureModem() {
        ITurtleAccess access = getAccess();
        if (!(access.getUpgrade(TurtleSide.RIGHT) instanceof TurtleModem)) {
            String modemId = advancedModem ? "wireless_modem_advanced" : "wireless_modem_normal";
            ItemStack modem = new ItemStack(advancedModem
                ? ModRegistry.Items.WIRELESS_MODEM_ADVANCED.get()
                : ModRegistry.Items.WIRELESS_MODEM_NORMAL.get());
            access.setUpgradeWithData(TurtleSide.RIGHT, UpgradeData.of(new TurtleModem(
                new net.minecraft.resources.ResourceLocation("computercraft", modemId), modem, advancedModem), new CompoundTag()));
        }
    }

    @Override public boolean canUse(Player player) { return true; }
    @Override public String getScript() { return script; }
    @Override public String getStatus() { return status; }
    @Override public String getError() { return error; }

    @Override
    public boolean setScript(String value) {
        String normalized = value == null ? "" : value.replace("\r\n", "\n").replace('\r', '\n');
        if (normalized.getBytes(StandardCharsets.UTF_8).length > RomComputerAccess.MAX_SCRIPT_BYTES) return false;
        script = normalized;
        error = "";
        status = script.isBlank() ? "waiting for configuration" : "waiting for redstone";
        sync();
        return true;
    }

    @Override
    public void startConfiguredProgram() {
        if (script.isBlank()) { reportFailure("No startup script is configured"); return; }
        if (getServerComputer() != null) unload();
        ServerComputer computer = createServerComputer();
        try {
            WritableMount mount = computer.createRootMount();
            clearMount(mount);
            writeFile(mount, "startup.lua", launcher());
            writeFile(mount, SCRIPT_FILE, script);
            writeFile(mount, STATUS_FILE, "ready");
            observedStatus = "ready";
            status = "starting";
            error = "";
            sync();
            getLevel().playSound(null, getBlockPos(), SoundEvents.NOTE_BLOCK_HARP.value(), SoundSource.BLOCKS, 0.65F, 1.2F);
            computer.turnOn();
        } catch (IOException | NullPointerException exception) {
            reportFailure("Unable to prepare computer storage");
        }
    }

    @Override public void shutdownComputer() {
        if (getServerComputer() != null) getServerComputer().shutdown();
        status = "stopped"; error = ""; sync();
    }

    @Override public void reportInvalidScript() { reportFailure("Startup script exceeds 16 KiB"); }

    @Override public void handleTerminalInput(RomComputerTerminalInputPacket packet) {
        ServerComputer computer = getServerComputer();
        if (computer == null || !computer.isOn() || status.equals("completed") || status.equals("error") || status.equals("stopped")) return;
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

    @Override public void copyConfigurationToItem(ItemStack stack) { if (!script.isBlank()) stack.getOrCreateTag().putString(ITEM_SCRIPT, script); }
    @Override public void readConfigurationFromItem(ItemStack stack) { if (stack.hasTag() && stack.getTag().contains(ITEM_SCRIPT)) setScript(stack.getTag().getString(ITEM_SCRIPT)); }

    @Override public Component getDisplayName() { return Component.translatable(getDisplayNameKey()); }

    protected String getDisplayNameKey() {
        return "block.vs_electronic_warfare.rom_turtle";
    }
    @Override public void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); tag.putString(NBT_SCRIPT, script); }
    @Override public void loadServer(CompoundTag tag) { super.loadServer(tag); script = tag.getString(NBT_SCRIPT); status = script.isBlank() ? "waiting for configuration" : "waiting for redstone"; activationConsumed = false; previousPowered = false; }
    @Override public void loadClient(CompoundTag tag) { super.loadClient(tag); script = tag.getString(NBT_SCRIPT); status = tag.getString(NBT_STATUS); error = tag.getString(NBT_ERROR); }
    @Override public CompoundTag getUpdateTag() { CompoundTag tag = super.getUpdateTag(); tag.putString(NBT_SCRIPT, script); tag.putString(NBT_STATUS, status); tag.putString(NBT_ERROR, error); return tag; }

    private void pollLauncherStatus() {
        ServerComputer computer = getServerComputer(); if (computer == null) return;
        try {
            WritableMount mount = computer.createRootMount(); if (mount == null || !mount.exists(STATUS_FILE)) return;
            String value = readFile(mount, STATUS_FILE); if (value.isBlank() || value.equals(observedStatus)) return;
            observedStatus = value;
            if (value.startsWith("running:")) { status = "running line " + value.substring(8); error = ""; }
            else if (value.equals("completed")) { status = value; error = ""; }
            else if (value.startsWith("error:")) { reportFailure(value.substring(6)); return; }
            sync();
        } catch (IOException ignored) { }
    }

    private void reportFailure(String message) { status = "error"; error = message; sync(); getLevel().playSound(null, getBlockPos(), SoundEvents.NOTE_BLOCK_HARP.value(), SoundSource.BLOCKS, 0.65F, 0.6F); }
    private void sync() { setChanged(); if (getLevel() != null) getLevel().sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_INVISIBLE); }
    private void syncTerminal() { ServerComputer computer = getServerComputer(); if (computer != null) ModNetwork.CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(getBlockPos().getX() + .5, getBlockPos().getY() + .5, getBlockPos().getZ() + .5, 8, getLevel().dimension())), new RomComputerTerminalPacket(getBlockPos(), computer.isOn() && !status.equals("completed") && !status.equals("error") && !status.equals("stopped"), computer.getTerminalState())); }
    private static void writeFile(WritableMount mount, String path, String content) throws IOException { try (var channel = mount.openForWrite(path)) { channel.write(ByteBuffer.wrap(content.getBytes(StandardCharsets.UTF_8))); } }
    private static String readFile(Mount mount, String path) throws IOException { ByteBuffer buffer = ByteBuffer.allocate((int) Math.min(mount.getSize(path), 512)); try (var channel = mount.openForRead(path)) { channel.read(buffer); } return StandardCharsets.UTF_8.decode((ByteBuffer) buffer.flip()).toString().trim(); }
    private static void clearMount(WritableMount mount) throws IOException { List<String> entries = new ArrayList<>(); mount.list("", entries); for (String entry : entries) mount.delete(entry); }
    private static String launcher() { return "local function status(v) local f=fs.open('" + STATUS_FILE + "','w') if f then f.write(v) f.close() end end\nlocal f=fs.open('" + SCRIPT_FILE + "','r') if not f then status('error:Cannot open startup script') return end\nlocal n=0 while true do local l=f.readLine() if not l then break end n=n+1 local c=l:match('^%s*(.-)%s*$') if c~='' and c:sub(1,1)~='#' then status('running:'..n) if not shell.run(c) then status('error:'..c:sub(1,180)) f.close() return end end end f.close() status('completed')\n"; }

}
