package com.lauya.vselectronicwarfare.menu;

import com.lauya.vselectronicwarfare.block.entity.RomComputerBlockEntity;
import com.lauya.vselectronicwarfare.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class RomComputerMenu extends AbstractContainerMenu {
    private final BlockPos blockPos;
    private final String initialScript;
    private final String initialStatus;
    private final String initialError;

    public RomComputerMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        this(containerId, inventory, buffer.readBlockPos(), buffer.readUtf(RomComputerBlockEntity.MAX_SCRIPT_BYTES), buffer.readUtf(128), buffer.readUtf(256));
    }

    public RomComputerMenu(int containerId, Inventory inventory, BlockPos blockPos) {
        this(containerId, inventory, blockPos, "", "", "");
    }

    private RomComputerMenu(int containerId, Inventory inventory, BlockPos blockPos, String initialScript, String initialStatus, String initialError) {
        super(ModMenus.ROM_COMPUTER.get(), containerId);
        this.blockPos = blockPos;
        this.initialScript = initialScript;
        this.initialStatus = initialStatus;
        this.initialError = initialError;
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public String getInitialScript() {
        return initialScript;
    }

    public String getInitialStatus() {
        return initialStatus;
    }

    public String getInitialError() {
        return initialError;
    }

    @Nullable
    public RomComputerBlockEntity getRomComputer(Player player) {
        return player.level().getBlockEntity(blockPos) instanceof RomComputerBlockEntity computer ? computer : null;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(blockPos.getX() + 0.5D, blockPos.getY() + 0.5D, blockPos.getZ() + 0.5D) <= 64.0D
            && getRomComputer(player) != null;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
