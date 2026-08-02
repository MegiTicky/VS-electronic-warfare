package com.lauya.vselectronicwarfare.integration.cc;

import com.lauya.vselectronicwarfare.block.entity.RadarBlockEntity;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class RadarPeripheral implements IPeripheral {
    private final RadarBlockEntity radar;

    public RadarPeripheral(RadarBlockEntity radar) {
        this.radar = radar;
    }

    @Override
    public String getType() {
        return "vs_ew_radar";
    }

    @LuaFunction(mainThread = true)
    public List<Map<String, Object>> scan(Optional<Double> radius) throws LuaException {
        return RadarScanner.scan(radar, radius, RadarScanner.ScanMode.ALL);
    }

    @LuaFunction(mainThread = true)
    public List<Map<String, Object>> scanForEntities(Optional<Double> radius) throws LuaException {
        return RadarScanner.scan(radar, radius, RadarScanner.ScanMode.ENTITIES);
    }

    @LuaFunction(mainThread = true)
    public List<Map<String, Object>> scanForPlayers(Optional<Double> radius) throws LuaException {
        return RadarScanner.scan(radar, radius, RadarScanner.ScanMode.PLAYERS);
    }

    @LuaFunction(mainThread = true)
    public List<Map<String, Object>> scanForShips(Optional<Double> radius) throws LuaException {
        return RadarScanner.scan(radar, radius, RadarScanner.ScanMode.SHIPS);
    }

    @LuaFunction
    public Map<String, Object> getConfigInfo() {
        return RadarScanner.getConfigInfo();
    }

    @Override
    public Object getTarget() {
        return radar;
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        return other instanceof RadarPeripheral peripheral && peripheral.radar == radar;
    }
}
