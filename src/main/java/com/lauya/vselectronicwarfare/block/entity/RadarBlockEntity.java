package com.lauya.vselectronicwarfare.block.entity;

import com.lauya.vselectronicwarfare.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class RadarBlockEntity extends BlockEntity {
    public RadarBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.RADAR.get(), pos, blockState);
    }
}
