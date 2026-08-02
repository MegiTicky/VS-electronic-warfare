package com.lauya.vselectronicwarfare.network;

import com.lauya.vselectronicwarfare.VSElectronicWarfare;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(VSElectronicWarfare.MOD_ID, "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        CHANNEL.registerMessage(0, RomComputerConfigPacket.class, RomComputerConfigPacket::encode,
            RomComputerConfigPacket::decode, RomComputerConfigPacket::handle);
    }

    private ModNetwork() {
    }
}
