package com.lauya.vselectronicwarfare.block;

import com.lauya.vselectronicwarfare.block.entity.RomComputerAccess;
import com.lauya.vselectronicwarfare.block.entity.RomTurtleBlockEntity;
import com.lauya.vselectronicwarfare.menu.RomComputerMenu;
import com.lauya.vselectronicwarfare.registry.ModItems;
import dan200.computercraft.shared.computer.blocks.AbstractComputerBlockEntity;
import dan200.computercraft.shared.platform.RegistryEntry;
import dan200.computercraft.shared.turtle.blocks.TurtleBlock;
import dan200.computercraft.shared.turtle.blocks.TurtleBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public final class RomTurtleBlock extends TurtleBlock {
    public RomTurtleBlock(Properties properties, RegistryEntry<BlockEntityType<TurtleBlockEntity>> type) { super(properties, type); }

    @Override
    protected ItemStack getItem(AbstractComputerBlockEntity blockEntity) {
        ItemStack stack = super.getItem(blockEntity);
        if (blockEntity instanceof RomTurtleBlockEntity turtle) turtle.copyConfigurationToItem(stack);
        return stack;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!player.isSecondaryUseActive()) return super.use(state, level, pos, player, hand, hit);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof RomTurtleBlockEntity turtle) {
            NetworkHooks.openScreen(serverPlayer, new net.minecraft.world.MenuProvider() {
                @Override public Component getDisplayName() { return turtle.getDisplayName(); }
                @Override public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inventory, Player player) { return new RomComputerMenu(id, inventory, pos); }
            }, buffer -> { buffer.writeBlockPos(pos); buffer.writeUtf(turtle.getScript(), RomComputerAccess.MAX_SCRIPT_BYTES); buffer.writeUtf(turtle.getStatus()); buffer.writeUtf(turtle.getError()); });
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof RomTurtleBlockEntity turtle) {
            turtle.readConfigurationFromItem(stack);
            turtle.ensureModem();
        }
    }
}
