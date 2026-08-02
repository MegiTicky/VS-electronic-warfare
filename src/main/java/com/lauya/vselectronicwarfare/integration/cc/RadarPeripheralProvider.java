package com.lauya.vselectronicwarfare.integration.cc;

import com.lauya.vselectronicwarfare.block.entity.RadarBlockEntity;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LazyOptional;

public final class RadarPeripheralProvider implements IPeripheralProvider {
    @Override
    public LazyOptional<IPeripheral> getPeripheral(Level world, BlockPos pos, Direction side) {
        if (world.getBlockEntity(pos) instanceof RadarBlockEntity radar) {
            return LazyOptional.of(() -> new RadarPeripheral(radar));
        }
        return LazyOptional.empty();
    }
}
