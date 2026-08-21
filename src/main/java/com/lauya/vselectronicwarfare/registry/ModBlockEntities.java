package com.lauya.vselectronicwarfare.registry;

import com.lauya.vselectronicwarfare.VSElectronicWarfare;
import com.lauya.vselectronicwarfare.block.entity.RadarBlockEntity;
import com.lauya.vselectronicwarfare.block.entity.RomComputerBlockEntity;
import com.lauya.vselectronicwarfare.block.entity.RomCommandComputerBlockEntity;
import com.lauya.vselectronicwarfare.block.entity.RomTurtleBlockEntity;
import com.lauya.vselectronicwarfare.block.entity.RomAdvancedTurtleBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, VSElectronicWarfare.MOD_ID);

    public static final RegistryObject<BlockEntityType<RadarBlockEntity>> RADAR = BLOCK_ENTITIES.register("radar",
        () -> BlockEntityType.Builder.of(RadarBlockEntity::new, ModBlocks.RADAR.get()).build(null));

    public static final RegistryObject<BlockEntityType<RomComputerBlockEntity>> ROM_COMPUTER = BLOCK_ENTITIES.register("rom_computer",
        () -> BlockEntityType.Builder.of(RomComputerBlockEntity::new, ModBlocks.ROM_COMPUTER.get()).build(null));

    public static final RegistryObject<BlockEntityType<RomCommandComputerBlockEntity>> ROM_COMMAND_COMPUTER = BLOCK_ENTITIES.register("rom_command_computer",
        () -> BlockEntityType.Builder.of(RomCommandComputerBlockEntity::new, ModBlocks.ROM_COMMAND_COMPUTER.get()).build(null));

    public static final RegistryObject<BlockEntityType<RomTurtleBlockEntity>> ROM_TURTLE = BLOCK_ENTITIES.register("rom_turtle",
        () -> BlockEntityType.Builder.of(RomTurtleBlockEntity::new, ModBlocks.ROM_TURTLE.get()).build(null));

    public static final RegistryObject<BlockEntityType<RomAdvancedTurtleBlockEntity>> ROM_ADVANCED_TURTLE = BLOCK_ENTITIES.register("rom_advanced_turtle",
        () -> BlockEntityType.Builder.of(RomAdvancedTurtleBlockEntity::new, ModBlocks.ROM_ADVANCED_TURTLE.get()).build(null));

    private ModBlockEntities() {
    }
}
