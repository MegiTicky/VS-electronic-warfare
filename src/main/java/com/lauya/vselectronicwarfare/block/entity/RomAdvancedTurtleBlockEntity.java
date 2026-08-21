package com.lauya.vselectronicwarfare.block.entity;

import com.lauya.vselectronicwarfare.registry.ModBlockEntities;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public final class RomAdvancedTurtleBlockEntity extends RomTurtleBlockEntity {
    public RomAdvancedTurtleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ROM_ADVANCED_TURTLE.get(), pos, state, ComputerFamily.ADVANCED, true);
    }

    @Override
    protected String getDisplayNameKey() {
        return "block.vs_electronic_warfare.rom_advanced_turtle";
    }
}
