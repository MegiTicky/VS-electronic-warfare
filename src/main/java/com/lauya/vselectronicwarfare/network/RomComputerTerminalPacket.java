package com.lauya.vselectronicwarfare.network;

import com.lauya.vselectronicwarfare.client.RomComputerScreen;
import dan200.computercraft.shared.computer.terminal.TerminalState;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record RomComputerTerminalPacket(BlockPos blockPos, boolean interactive, TerminalState terminal) {
    public static void encode(RomComputerTerminalPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.blockPos);
        buffer.writeBoolean(packet.interactive);
        packet.terminal.write(buffer);
    }

    public static RomComputerTerminalPacket decode(FriendlyByteBuf buffer) {
        return new RomComputerTerminalPacket(buffer.readBlockPos(), buffer.readBoolean(), new TerminalState(buffer));
    }

    public static void handle(RomComputerTerminalPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClient(packet)));
        context.setPacketHandled(true);
    }

    private static void handleClient(RomComputerTerminalPacket packet) {
        if (Minecraft.getInstance().screen instanceof RomComputerScreen screen) screen.updateTerminal(packet);
    }
}
