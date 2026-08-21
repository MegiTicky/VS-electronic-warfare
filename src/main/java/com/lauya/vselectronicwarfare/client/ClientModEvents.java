package com.lauya.vselectronicwarfare.client;

import com.lauya.vselectronicwarfare.VSElectronicWarfare;
import com.lauya.vselectronicwarfare.registry.ModMenus;
import com.lauya.vselectronicwarfare.registry.ModBlockEntities;
import dan200.computercraft.client.render.TurtleBlockEntityRenderer;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;

@Mod.EventBusSubscriber(modid = VSElectronicWarfare.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(ModMenus.ROM_COMPUTER.get(), RomComputerScreen::new));
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.ROM_TURTLE.get(), TurtleBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.ROM_ADVANCED_TURTLE.get(), TurtleBlockEntityRenderer::new);
    }
}
