package com.lauya.vselectronicwarfare.registry;

import com.lauya.vselectronicwarfare.VSElectronicWarfare;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, VSElectronicWarfare.MOD_ID);

    public static final RegistryObject<CreativeModeTab> TAB = CREATIVE_MODE_TABS.register("tab",
        () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.vs_electronic_warfare"))
            .icon(() -> new ItemStack(ModItems.RADAR.get()))
            .displayItems((parameters, output) -> {
                output.accept(ModItems.RADAR.get());
                output.accept(ModItems.ROM_COMPUTER.get());
            })
            .build());

    private ModCreativeTabs() {
    }
}
