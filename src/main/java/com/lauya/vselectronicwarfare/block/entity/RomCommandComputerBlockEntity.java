package com.lauya.vselectronicwarfare.block.entity;

import com.lauya.vselectronicwarfare.registry.ModBlockEntities;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public final class RomCommandComputerBlockEntity extends RomComputerBlockEntity {
    public RomCommandComputerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ROM_COMMAND_COMPUTER.get(), pos, state, ComputerFamily.COMMAND);
    }

    @Override
    public boolean isOperatorOnly() {
        return true;
    }

    @Override
    protected String getDisplayNameKey() {
        return "block.vs_electronic_warfare.rom_command_computer";
    }
}
