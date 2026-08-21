package com.lauya.vselectronicwarfare.registry;

import com.lauya.vselectronicwarfare.VSElectronicWarfare;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import com.lauya.vselectronicwarfare.item.RomTurtleItem;
import dan200.computercraft.shared.computer.items.CommandComputerItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, VSElectronicWarfare.MOD_ID);

    public static final RegistryObject<Item> RADAR = ITEMS.register("radar",
        () -> new BlockItem(ModBlocks.RADAR.get(), new Item.Properties()));

    public static final RegistryObject<Item> ROM_COMPUTER = ITEMS.register("rom_computer",
        () -> new BlockItem(ModBlocks.ROM_COMPUTER.get(), new Item.Properties()));

    public static final RegistryObject<Item> ROM_COMMAND_COMPUTER = ITEMS.register("rom_command_computer",
        () -> new CommandComputerItem(ModBlocks.ROM_COMMAND_COMPUTER.get(), new Item.Properties()));

    public static final RegistryObject<Item> ROM_TURTLE = ITEMS.register("rom_turtle",
        () -> new RomTurtleItem(ModBlocks.ROM_TURTLE.get(), new Item.Properties()));

    public static final RegistryObject<Item> ROM_ADVANCED_TURTLE = ITEMS.register("rom_advanced_turtle",
        () -> new RomTurtleItem(ModBlocks.ROM_ADVANCED_TURTLE.get(), new Item.Properties()));

    private ModItems() {
    }
}
