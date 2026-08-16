package com.lauya.vselectronicwarfare.block;

import com.lauya.vselectronicwarfare.block.entity.RomCommandComputerBlockEntity;
import com.lauya.vselectronicwarfare.block.entity.RomComputerBlockEntity;
import com.lauya.vselectronicwarfare.registry.ModItems;
import dan200.computercraft.shared.computer.blocks.AbstractComputerBlockEntity;
import dan200.computercraft.shared.computer.blocks.CommandComputerBlock;
import dan200.computercraft.shared.platform.RegistryEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class RomCommandComputerBlock extends CommandComputerBlock<RomCommandComputerBlockEntity> {
    public RomCommandComputerBlock(Properties properties, RegistryEntry<BlockEntityType<RomCommandComputerBlockEntity>> type) {
        super(properties, type);
    }

    @Override
    protected ItemStack getItem(AbstractComputerBlockEntity blockEntity) {
        ItemStack stack = new ItemStack(ModItems.ROM_COMMAND_COMPUTER.get());
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
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!player.hasPermissions(2)) return InteractionResult.PASS;
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
            && level.getBlockEntity(pos) instanceof RomCommandComputerBlockEntity romComputer) {
            NetworkHooks.openScreen(serverPlayer, romComputer, buffer -> {
                buffer.writeBlockPos(pos);
                buffer.writeUtf(romComputer.getScript(), RomComputerBlockEntity.MAX_SCRIPT_BYTES);
                buffer.writeUtf(romComputer.getStatus());
                buffer.writeUtf(romComputer.getError());
            });
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.hasPermissions(2)) return null;
        return super.getStateForPlacement(context);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
        Entity entity = builder.getOptionalParameter(LootContextParams.THIS_ENTITY);
        if (entity instanceof Player player && !player.hasPermissions(2)) return List.of();
        return super.getDrops(state, builder);
    }
}
