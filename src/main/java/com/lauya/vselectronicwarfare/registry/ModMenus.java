package com.lauya.vselectronicwarfare.registry;

import com.lauya.vselectronicwarfare.VSElectronicWarfare;
import com.lauya.vselectronicwarfare.menu.RomComputerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
        DeferredRegister.create(ForgeRegistries.MENU_TYPES, VSElectronicWarfare.MOD_ID);

    public static final RegistryObject<MenuType<RomComputerMenu>> ROM_COMPUTER = MENUS.register("rom_computer",
        () -> IForgeMenuType.create(RomComputerMenu::new));

    private ModMenus() {
    }
}
