package com.lauya.vselectronicwarfare.network;

import com.lauya.vselectronicwarfare.block.entity.RomComputerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record RomComputerTerminalInputPacket(BlockPos blockPos, Action action, int key, boolean repeat, int x, int y, String text) {
    private static final int MAX_TEXT_BYTES = 16 * 1024;

    public enum Action {
        CHAR,
        PASTE,
        KEY_DOWN,
        KEY_UP,
        MOUSE_CLICK,
        MOUSE_UP,
        MOUSE_DRAG,
        MOUSE_SCROLL
    }

    public static void encode(RomComputerTerminalInputPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.blockPos);
        buffer.writeEnum(packet.action);
        buffer.writeVarInt(packet.key);
        buffer.writeBoolean(packet.repeat);
        buffer.writeVarInt(packet.x);
        buffer.writeVarInt(packet.y);
        buffer.writeUtf(packet.text, MAX_TEXT_BYTES);
    }

    public static RomComputerTerminalInputPacket decode(FriendlyByteBuf buffer) {
        return new RomComputerTerminalInputPacket(buffer.readBlockPos(), buffer.readEnum(Action.class), buffer.readVarInt(),
            buffer.readBoolean(), buffer.readVarInt(), buffer.readVarInt(), buffer.readUtf(MAX_TEXT_BYTES));
    }

    public static void handle(RomComputerTerminalInputPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || player.distanceToSqr(packet.blockPos.getX() + 0.5D, packet.blockPos.getY() + 0.5D, packet.blockPos.getZ() + 0.5D) > 64.0D) {
                return;
            }
            if (player.level().getBlockEntity(packet.blockPos) instanceof RomComputerBlockEntity computer && computer.canUse(player)) {
                computer.handleTerminalInput(packet);
            }
        });
        context.setPacketHandled(true);
    }
}
