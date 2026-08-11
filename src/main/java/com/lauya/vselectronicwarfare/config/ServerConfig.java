package com.lauya.vselectronicwarfare.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ServerConfig {
    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.DoubleValue MAX_RADAR_SCAN_RADIUS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("radar");
        MAX_RADAR_SCAN_RADIUS = builder
            .comment("Maximum radius in blocks that a radar scan may use.")
            .defineInRange("maxRadarScanRadius", 2048.0D, 1.0D, 32768.0D);
        builder.pop();
        SPEC = builder.build();
    }

    private ServerConfig() {
    }

    public static double maxRadarScanRadius() {
        return MAX_RADAR_SCAN_RADIUS.get();
    }
}
