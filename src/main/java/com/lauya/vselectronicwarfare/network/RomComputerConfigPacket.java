package com.lauya.vselectronicwarfare.network;

import com.lauya.vselectronicwarfare.block.entity.RomComputerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record RomComputerConfigPacket(BlockPos blockPos, Action action, String script) {
    public enum Action {
        SAVE,
        START,
        SHUTDOWN
    }

    public static void encode(RomComputerConfigPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.blockPos);
        buffer.writeEnum(packet.action);
        buffer.writeUtf(packet.script, RomComputerBlockEntity.MAX_SCRIPT_BYTES);
    }

    public static RomComputerConfigPacket decode(FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        Action action = buffer.readEnum(Action.class);
        String script = buffer.readUtf(RomComputerBlockEntity.MAX_SCRIPT_BYTES);
        return new RomComputerConfigPacket(pos, action, script);
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
                    if (!computer.setScript(packet.script)) computer.reportInvalidScript();
                }
                case START -> {
                    if (!computer.setScript(packet.script)) {
                        computer.reportInvalidScript();
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
