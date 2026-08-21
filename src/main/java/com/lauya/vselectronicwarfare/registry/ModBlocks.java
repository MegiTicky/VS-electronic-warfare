package com.lauya.vselectronicwarfare.registry;

import com.lauya.vselectronicwarfare.VSElectronicWarfare;
import com.lauya.vselectronicwarfare.block.RadarBlock;
import com.lauya.vselectronicwarfare.block.RomComputerBlock;
import com.lauya.vselectronicwarfare.block.RomCommandComputerBlock;
import com.lauya.vselectronicwarfare.block.RomTurtleBlock;
import dan200.computercraft.shared.platform.RegistryEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import dan200.computercraft.shared.turtle.blocks.TurtleBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
        DeferredRegister.create(ForgeRegistries.BLOCKS, VSElectronicWarfare.MOD_ID);

    public static final RegistryObject<Block> RADAR = BLOCKS.register("radar",
        () -> new RadarBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(2.0f, 6.0f)
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> ROM_COMPUTER = BLOCKS.register("rom_computer",
        () -> new RomComputerBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(2.0f, 6.0f)
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops(), new RegistryEntry<>() {
                @Override
                public BlockEntityType<com.lauya.vselectronicwarfare.block.entity.RomComputerBlockEntity> get() {
                    return ModBlockEntities.ROM_COMPUTER.get();
                }

                @Override
                public ResourceLocation id() {
                    return new ResourceLocation(VSElectronicWarfare.MOD_ID, "rom_computer");
                }
            }));

    public static final RegistryObject<RomCommandComputerBlock> ROM_COMMAND_COMPUTER = BLOCKS.register("rom_command_computer",
        () -> new RomCommandComputerBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(2.0f, 6.0f)
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops(), new RegistryEntry<>() {
                @Override
                public BlockEntityType<com.lauya.vselectronicwarfare.block.entity.RomCommandComputerBlockEntity> get() {
                    return ModBlockEntities.ROM_COMMAND_COMPUTER.get();
                }

                @Override
                public ResourceLocation id() {
                    return new ResourceLocation(VSElectronicWarfare.MOD_ID, "rom_command_computer");
                }
            }));

    public static final RegistryObject<RomTurtleBlock> ROM_TURTLE = BLOCKS.register("rom_turtle",
        () -> new RomTurtleBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(2.0f, 6.0f)
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops(), new RegistryEntry<>() {
                @Override
                @SuppressWarnings("unchecked")
                public BlockEntityType<TurtleBlockEntity> get() {
                    return (BlockEntityType<TurtleBlockEntity>) (BlockEntityType<?>) ModBlockEntities.ROM_TURTLE.get();
                }

                @Override
                public ResourceLocation id() {
                    return new ResourceLocation(VSElectronicWarfare.MOD_ID, "rom_turtle");
                }
            }));

    public static final RegistryObject<RomTurtleBlock> ROM_ADVANCED_TURTLE = BLOCKS.register("rom_advanced_turtle",
        () -> new RomTurtleBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(2.0f, 6.0f)
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops(), new RegistryEntry<>() {
                @Override
                @SuppressWarnings("unchecked")
                public BlockEntityType<TurtleBlockEntity> get() {
                    return (BlockEntityType<TurtleBlockEntity>) (BlockEntityType<?>) ModBlockEntities.ROM_ADVANCED_TURTLE.get();
                }

                @Override
                public ResourceLocation id() {
                    return new ResourceLocation(VSElectronicWarfare.MOD_ID, "rom_advanced_turtle");
                }
            }));

    private ModBlocks() {
    }
}
