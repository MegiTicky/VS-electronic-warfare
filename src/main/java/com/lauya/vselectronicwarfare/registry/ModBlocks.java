package com.lauya.vselectronicwarfare.registry;

import com.lauya.vselectronicwarfare.VSElectronicWarfare;
import com.lauya.vselectronicwarfare.block.RadarBlock;
import com.lauya.vselectronicwarfare.block.RomComputerBlock;
import dan200.computercraft.shared.platform.RegistryEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;
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

    private ModBlocks() {
    }
}
