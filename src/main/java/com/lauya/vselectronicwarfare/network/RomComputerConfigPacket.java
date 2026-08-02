package com.lauya.vselectronicwarfare.network;

import com.lauya.vselectronicwarfare.block.entity.RomComputerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record RomComputerConfigPacket(BlockPos blockPos, Action action, String pastebinId) {
    public enum Action {
        SAVE,
        START,
        SHUTDOWN
    }

    public static void encode(RomComputerConfigPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.blockPos);
        buffer.writeEnum(packet.action);
        buffer.writeUtf(packet.pastebinId, 128);
    }

    public static RomComputerConfigPacket decode(FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        Action action = buffer.readEnum(Action.class);
        String pastebinId = buffer.readUtf(128);
        return new RomComputerConfigPacket(pos, action, pastebinId);
    }

    public static void handle(RomComputerConfigPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || player.distanceToSqr(packet.blockPos.getX() + 0.5D, packet.blockPos.getY() + 0.5D, packet.blockPos.getZ() + 0.5D) > 64.0D) {
                return;
            }
            if (!(player.level().getBlockEntity(packet.blockPos) instanceof RomComputerBlockEntity computer)) return;

            switch (packet.action) {
                case SAVE -> {
                    if (!computer.setPastebinId(packet.pastebinId)) computer.reportInvalidProgram();
                }
                case START -> {
                    if (!computer.setPastebinId(packet.pastebinId)) {
                        computer.reportInvalidProgram();
                        return;
                    }
                    computer.startConfiguredProgram();
                }
                case SHUTDOWN -> computer.shutdownComputer();
            }
        });
        context.setPacketHandled(true);
    }
}
