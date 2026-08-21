package com.lauya.vselectronicwarfare.block.entity;

import com.lauya.vselectronicwarfare.network.RomComputerTerminalInputPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface RomComputerAccess {
    int MAX_SCRIPT_BYTES = 16 * 1024;

    BlockPos getBlockPos();
    String getScript();
    String getStatus();
    String getError();
    boolean canUse(Player player);
    boolean setScript(String value);
    void reportInvalidScript();
    void startConfiguredProgram();
    void shutdownComputer();
    void handleTerminalInput(RomComputerTerminalInputPacket packet);
    void copyConfigurationToItem(ItemStack stack);
    void readConfigurationFromItem(ItemStack stack);
}
