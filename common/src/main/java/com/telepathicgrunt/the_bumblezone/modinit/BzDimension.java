package com.telepathicgrunt.the_bumblezone.modinit;

import com.mojang.serialization.MapCodec;
import com.teamresourceful.resourcefullib.common.registry.RegistryEntry;
import com.teamresourceful.resourcefullib.common.registry.ResourcefulRegistries;
import com.teamresourceful.resourcefullib.common.registry.ResourcefulRegistry;
import com.telepathicgrunt.the_bumblezone.Bumblezone;
import com.telepathicgrunt.the_bumblezone.worldgen.dimension.BzBiomeSource;
import com.telepathicgrunt.the_bumblezone.worldgen.dimension.BzChunkGenerator;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.DensityFunction;

public class BzDimension {
    public static final ResourceKey<Level> BZ_WORLD_KEY = ResourceKey.create(Registries.DIMENSION, Bumblezone.MOD_DIMENSION_ID);

    public static final ResourcefulRegistry<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATOR = ResourcefulRegistries.create(BuiltInRegistries.CHUNK_GENERATOR, Bumblezone.MODID);
    public static final ResourcefulRegistry<MapCodec<? extends BiomeSource>> BIOME_SOURCE = ResourcefulRegistries.create(BuiltInRegistries.BIOME_SOURCE, Bumblezone.MODID);
    public static final ResourcefulRegistry<MapCodec<? extends DensityFunction>> DENSITY_FUNCTIONS = ResourcefulRegistries.create(BuiltInRegistries.DENSITY_FUNCTION_TYPE, Bumblezone.MODID);

    public static final RegistryEntry<MapCodec<BzChunkGenerator>> BZ_CHUNK_GENERATOR = CHUNK_GENERATOR.register("chunk_generator", () -> BzChunkGenerator.CODEC);
    public static final RegistryEntry<MapCodec<BzBiomeSource>> BZ_BIOME_SOURCE = BIOME_SOURCE.register("biome_source", () -> BzBiomeSource.CODEC);
    public static final RegistryEntry<MapCodec<BzChunkGenerator.BiomeNoise>> BZ_BIOME_FUNCTION = DENSITY_FUNCTIONS.register("biome_function", BzChunkGenerator.BiomeNoise.CODEC::codec);
}
