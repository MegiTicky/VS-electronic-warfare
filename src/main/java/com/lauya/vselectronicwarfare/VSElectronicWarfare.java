package com.lauya.vselectronicwarfare;

import com.lauya.vselectronicwarfare.integration.cc.RadarPeripheralProvider;
import com.lauya.vselectronicwarfare.network.ModNetwork;
import com.lauya.vselectronicwarfare.config.ServerConfig;
import com.lauya.vselectronicwarfare.registry.ModBlockEntities;
import com.lauya.vselectronicwarfare.registry.ModBlocks;
import com.lauya.vselectronicwarfare.registry.ModCreativeTabs;
import com.lauya.vselectronicwarfare.registry.ModItems;
import com.lauya.vselectronicwarfare.registry.ModMenus;
import com.mojang.logging.LogUtils;
import dan200.computercraft.api.ForgeComputerCraftAPI;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(VSElectronicWarfare.MOD_ID)
public final class VSElectronicWarfare {
    public static final String MOD_ID = "vs_electronic_warfare";
    public static final Logger LOGGER = LogUtils.getLogger();

    public VSElectronicWarfare() {
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModMenus.MENUS.register(modBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modBus);
        ModNetwork.register();
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
        modBus.addListener(this::commonSetup);

        LOGGER.info("Loading VS: Electronic Warfare radar");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ForgeComputerCraftAPI.registerPeripheralProvider(new RadarPeripheralProvider());
            LOGGER.info("Registered VS: Electronic Warfare radar ComputerCraft peripheral provider");
        });
    }
}
