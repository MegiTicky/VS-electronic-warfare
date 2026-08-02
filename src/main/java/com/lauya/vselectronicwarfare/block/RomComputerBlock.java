package com.lauya.vselectronicwarfare.block;

import com.lauya.vselectronicwarfare.block.entity.RomComputerBlockEntity;
import com.lauya.vselectronicwarfare.registry.ModItems;
import dan200.computercraft.shared.computer.blocks.AbstractComputerBlockEntity;
import dan200.computercraft.shared.computer.blocks.ComputerBlock;
import dan200.computercraft.shared.platform.RegistryEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public final class RomComputerBlock extends ComputerBlock<RomComputerBlockEntity> {
    public RomComputerBlock(Properties properties, RegistryEntry<BlockEntityType<RomComputerBlockEntity>> type) {
        super(properties, type);
    }

    @Override
    protected ItemStack getItem(AbstractComputerBlockEntity blockEntity) {
        ItemStack stack = new ItemStack(ModItems.ROM_COMPUTER.get());
        if (blockEntity instanceof RomComputerBlockEntity romComputer) romComputer.copyConfigurationToItem(stack);
        return stack;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof RomComputerBlockEntity romComputer) {
            romComputer.readConfigurationFromItem(stack);
        }
    }

    @Override
    @Deprecated
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
            && level.getBlockEntity(pos) instanceof RomComputerBlockEntity romComputer) {
            NetworkHooks.openScreen(serverPlayer, romComputer, buffer -> {
                buffer.writeBlockPos(pos);
                buffer.writeUtf(romComputer.getPastebinId());
                buffer.writeUtf(romComputer.getStatus());
                buffer.writeUtf(romComputer.getError());
            });
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return super.getStateForPlacement(context);
    }
}
