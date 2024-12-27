package com.telepathicgrunt.the_bumblezone.worldgen.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.telepathicgrunt.the_bumblezone.Bumblezone;
import com.telepathicgrunt.the_bumblezone.utils.GeneralUtils;
import com.telepathicgrunt.the_bumblezone.worldgen.dimension.layer.BzBiomeBlobLayer;
import com.telepathicgrunt.the_bumblezone.worldgen.dimension.layer.BzBiomeLayer;
import com.telepathicgrunt.the_bumblezone.worldgen.dimension.layer.BzBiomeMergeLayer;
import com.telepathicgrunt.the_bumblezone.worldgen.dimension.layer.BzBiomePillarLayer;
import com.telepathicgrunt.the_bumblezone.worldgen.dimension.layer.BzBiomePollinatedFieldsLayer;
import com.telepathicgrunt.the_bumblezone.worldgen.dimension.layer.BzBiomePollinatedPillarLayer;
import com.telepathicgrunt.the_bumblezone.worldgen.dimension.layer.BzBiomeScaleLayer;
import com.telepathicgrunt.the_bumblezone.worldgen.dimension.layer.vanilla.Area;
import com.telepathicgrunt.the_bumblezone.worldgen.dimension.layer.vanilla.AreaFactory;
import com.telepathicgrunt.the_bumblezone.worldgen.dimension.layer.vanilla.AreaTransformer1;
import com.telepathicgrunt.the_bumblezone.worldgen.dimension.layer.vanilla.BigContext;
import com.telepathicgrunt.the_bumblezone.worldgen.dimension.layer.vanilla.Layer;
import com.telepathicgrunt.the_bumblezone.worldgen.dimension.layer.vanilla.LazyArea;
import com.telepathicgrunt.the_bumblezone.worldgen.dimension.layer.vanilla.LazyAreaContext;
import com.telepathicgrunt.the_bumblezone.worldgen.dimension.layer.vanilla.ZoomLayer;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.Set;
import java.util.function.LongFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BzBiomeSource extends BiomeSource implements BiomeManager.NoiseBiomeSource {

    public static final MapCodec<BzBiomeSource> CODEC =
            RecordCodecBuilder.mapCodec((instance) -> instance.group(
                Codec.LONG.fieldOf("seed").orElse(0L).stable().forGetter(bzBiomeProvider -> bzBiomeProvider.seed),
                Biome.LIST_CODEC.fieldOf("blob_biomes").orElse(HolderSet.direct()).forGetter((biomeSource) -> biomeSource.blobBiomes),
                Biome.LIST_CODEC.fieldOf("rare_blob_biomes").orElse(HolderSet.direct()).forGetter((biomeSource) -> biomeSource.rareBlobBiomes),
                Biome.LIST_CODEC.fieldOf("main_biomes").orElse(HolderSet.direct()).forGetter((biomeSource) -> biomeSource.mainBiomes))
            .apply(instance, instance.stable(BzBiomeSource::new)));

    public static final ResourceLocation HIVE_WALL = ResourceLocation.fromNamespaceAndPath(Bumblezone.MODID, "hive_wall");
    public static final ResourceLocation HIVE_PILLAR = ResourceLocation.fromNamespaceAndPath(Bumblezone.MODID, "hive_pillar");
    public static final ResourceLocation SUGAR_WATER_FLOOR = ResourceLocation.fromNamespaceAndPath(Bumblezone.MODID, "sugar_water_floor");
    public static final ResourceLocation POLLINATED_FIELDS = ResourceLocation.fromNamespaceAndPath(Bumblezone.MODID, "pollinated_fields");
    public static final ResourceLocation POLLINATED_PILLAR = ResourceLocation.fromNamespaceAndPath(Bumblezone.MODID, "pollinated_pillar");
    public static final ResourceLocation CRYSTAL_CANYON = ResourceLocation.fromNamespaceAndPath(Bumblezone.MODID, "crystal_canyon");

    private final long seed;
    private final Layer biomeSampler;
    public final HolderSet<Biome> blobBiomes;
    public final HolderSet<Biome> rareBlobBiomes;
    public final HolderSet<Biome> mainBiomes;
    public final GeneralUtils.Lazy<Set<Holder<Biome>>> lazyPossibleBiomes = new GeneralUtils.Lazy<>();

    public BzBiomeSource(long seed, HolderSet<Biome> blobBiomes, HolderSet<Biome> rareBlobBiomes, HolderSet<Biome> mainBiomes) {
        super();

        this.seed = seed;
        this.blobBiomes = blobBiomes;
        this.rareBlobBiomes = rareBlobBiomes;
        this.mainBiomes = mainBiomes;
        this.biomeSampler = buildWorldProcedure(seed, this.blobBiomes, this.rareBlobBiomes);
    }

    public BzBiomeSource(BzBiomeSource originalBiomeSource) {
        this.seed = originalBiomeSource.seed;
        this.blobBiomes = originalBiomeSource.blobBiomes;
        this.rareBlobBiomes = originalBiomeSource.rareBlobBiomes;
        this.mainBiomes = originalBiomeSource.mainBiomes;
        this.biomeSampler = buildWorldProcedure(seed, this.blobBiomes, this.rareBlobBiomes);
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return this.lazyPossibleBiomes.getOrCompute(() -> Stream.concat(Stream.concat(blobBiomes.stream(), mainBiomes.stream()), rareBlobBiomes.stream()).collect(Collectors.toSet())).stream();
    }

    @Override
    public Set<Holder<Biome>> possibleBiomes() {
        return this.lazyPossibleBiomes.getOrCompute(() -> Stream.concat(Stream.concat(blobBiomes.stream(), mainBiomes.stream()), rareBlobBiomes.stream()).collect(Collectors.toSet()));
    }
    
    public static <T extends Area, C extends BigContext<T>> AreaFactory<T> stack(long seed, AreaTransformer1 parent, AreaFactory<T> incomingArea, int count, LongFunction<C> contextFactory) {
        AreaFactory<T> LayerFactory = incomingArea;

        for (int i = 0; i < count; ++i) {
            LayerFactory = parent.run(contextFactory.apply(seed + (long) i), LayerFactory);
        }

        return LayerFactory;
    }

    public static Layer buildWorldProcedure(long seed, HolderSet<Biome> blobBiomes, HolderSet<Biome> rareBlobBiomes) {
        AreaFactory<LazyArea> layerFactory = build((salt) -> new LazyAreaContext(25, seed, salt), seed, blobBiomes, rareBlobBiomes);
        return new Layer(layerFactory);
    }

    public static <T extends Area, C extends BigContext<T>> AreaFactory<T> build(LongFunction<C> contextFactory, long seed, HolderSet<Biome> blobBiomes, HolderSet<Biome> rareBlobBiomes) {
        AreaFactory<T> layer = new BzBiomeLayer(seed).run(contextFactory.apply(200L));
        layer = new BzBiomePillarLayer().run(contextFactory.apply(1008L), layer);
        layer = new BzBiomeScaleLayer(Set.of(HIVE_PILLAR)).run(contextFactory.apply(1055L), layer);
        layer = ZoomLayer.FUZZY.run(contextFactory.apply(2003L), layer);
        layer = ZoomLayer.FUZZY.run(contextFactory.apply(2523L), layer);
        layer = new BzBiomeScaleLayer(Set.of(CRYSTAL_CANYON, SUGAR_WATER_FLOOR)).run(contextFactory.apply(54088L), layer);
        AreaFactory<T> layerOverlay = new BzBiomeBlobLayer(blobBiomes, rareBlobBiomes).run(contextFactory.apply(204L));
        layerOverlay = ZoomLayer.NORMAL.run(contextFactory.apply(2423L), layerOverlay);
        layerOverlay = new BzBiomePollinatedPillarLayer().run(contextFactory.apply(3008L), layerOverlay);
        layerOverlay = new BzBiomeScaleLayer(Set.of(POLLINATED_PILLAR)).run(contextFactory.apply(4455L), layerOverlay);
        layerOverlay = ZoomLayer.NORMAL.run(contextFactory.apply(2503L), layerOverlay);
        layerOverlay = ZoomLayer.NORMAL.run(contextFactory.apply(2603L), layerOverlay);
        layerOverlay = new BzBiomePollinatedFieldsLayer().run(contextFactory.apply(3578L), layerOverlay);
        layerOverlay = new BzBiomeScaleLayer(Set.of(POLLINATED_FIELDS)).run(contextFactory.apply(4055L), layerOverlay);
        layerOverlay = ZoomLayer.FUZZY.run(contextFactory.apply(2853L), layerOverlay);
        layerOverlay = ZoomLayer.FUZZY.run(contextFactory.apply(3583L), layerOverlay);
        layerOverlay = ZoomLayer.NORMAL.run(contextFactory.apply(4583L), layerOverlay);
        layer = new BzBiomeMergeLayer().run(contextFactory.apply(5583L), layerOverlay, layer);

        return layer;
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        return biomeSampler.sample(x, z);
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z) {
        return biomeSampler.sample(x, z);
    }
}
