package com.telepathicgrunt.the_bumblezone.worldgen.dimension;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.telepathicgrunt.the_bumblezone.Bumblezone;
import com.telepathicgrunt.the_bumblezone.mixin.world.NoiseChunkAccessor;
import com.telepathicgrunt.the_bumblezone.utils.PlatformHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.Beardifier;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.NoiseRouterData;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.apache.commons.lang3.mutable.MutableObject;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Predicate;

public class BzChunkGenerator extends NoiseBasedChunkGenerator {

    public static final MapCodec<BzChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(bzChunkGenerator -> bzChunkGenerator.biomeSource),
                    NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(bzChunkGenerator -> bzChunkGenerator.settings))
            .apply(instance, instance.stable(BzChunkGenerator::new)));

    protected final BlockState defaultBlock;
    protected final BlockState defaultFluid;
    private final Holder<NoiseGeneratorSettings> settings;
    private final Aquifer.FluidPicker globalFluidPicker;

    public BzChunkGenerator(BiomeSource biomeSource,
                            Holder<NoiseGeneratorSettings> supplier
    ) {
        super(biomeSource, supplier);
        NoiseGeneratorSettings noiseGeneratorSettings = supplier.value();
        this.defaultBlock = noiseGeneratorSettings.defaultBlock();
        this.defaultFluid = noiseGeneratorSettings.defaultFluid();
        NoiseRouter noiseRouter = noiseGeneratorSettings.noiseRouter();

        BiomeNoise.biomeSourceOriginal = this.getBiomeSource();
        BiomeNoise.sampler = new Climate.Sampler(
                noiseRouter.temperature(),
                noiseRouter.vegetation(),
                noiseRouter.continents(),
                noiseRouter.erosion(),
                noiseRouter.depth(),
                noiseRouter.ridges(),
                noiseGeneratorSettings.spawnTarget());

        this.settings = supplier;

        int seaLevel = noiseGeneratorSettings.seaLevel();
        Aquifer.FluidStatus sea = new Aquifer.FluidStatus(seaLevel, noiseGeneratorSettings.defaultFluid());
        this.globalFluidPicker = (x, y, z) -> sea;
    }

    public record BiomeNoise(BiomeSource biomeSource) implements DensityFunction.SimpleFunction {
        public static final KeyDispatchDataCodec<BiomeNoise> CODEC = KeyDispatchDataCodec.of(MapCodec.unit(new BiomeNoise()));
        public static Climate.Sampler sampler;
        public static BiomeSource biomeSourceOriginal;

        @Override
        public double compute(FunctionContext functionContext) {
            return BiomeInfluencedNoiseSampler.calculateBaseNoise(
                    functionContext.blockX(),
                    functionContext.blockZ(),
                    sampler,
                    biomeSource,
                    BiomeRegistryHolder.BIOME_REGISTRY);
        }

        @Override
        public DensityFunction mapAll(Visitor visitor) {
            BiomeSource source;
            if (biomeSourceOriginal instanceof BzBiomeSource bzBiomeSource) {
                source = new BzBiomeSource(bzBiomeSource);
            }
            else {
                source = biomeSourceOriginal;
            }
            return visitor.apply(new BiomeNoise(source));
        }

        @Override
        public double minValue() {
            return -10;
        }

        @Override
        public double maxValue() {
            return 10;
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    protected void doCreateBiomes(Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunkAccess) {
        BiomeResolver biomeresolver;
        if (this.biomeSource instanceof BzBiomeSource bzBiomeSource) {
            biomeresolver = getBiomeResolver(new BzBiomeSource(bzBiomeSource));
        }
        else {
            biomeresolver = getBiomeResolver(this.biomeSource);
        }
        NoiseChunk noisechunk = chunkAccess.getOrCreateNoiseChunk((noiseChunk) -> this.createNoiseChunk(noiseChunk, structureManager, blender, randomState));
        Climate.Sampler sampler = ((NoiseChunkAccessor)noisechunk).callCachedClimateSampler(randomState.router(), this.settings.value().spawnTarget());
        chunkAccess.fillBiomesFromNoise(biomeresolver, sampler);
    }

    private BiomeResolver getBiomeResolver(BiomeResolver biomeResolver) {
        return (x, y, z, biomeHolder) -> biomeResolver.getNoiseBiome(x, 0, z, biomeHolder);
    }

    private NoiseChunk createNoiseChunk(ChunkAccess chunkAccess, StructureManager structureManager, Blender blender, RandomState randomState) {
        return NoiseChunk.forChunk(chunkAccess, randomState, Beardifier.forStructuresInChunk(structureManager, chunkAccess.getPos()), this.settings.value(), this.globalFluidPicker, blender);
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types types, LevelHeightAccessor levelHeightAccessor, RandomState randomState) {
        return this.iterateNoiseColumn(levelHeightAccessor, randomState, x, z, null, types.isOpaque())
                .orElse(levelHeightAccessor.getMinBuildHeight());
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor levelHeightAccessor, RandomState randomState) {
        MutableObject<NoiseColumn> mutableobject = new MutableObject<>();
        this.iterateNoiseColumn(levelHeightAccessor, randomState, x, z, mutableobject, null);
        return mutableobject.getValue();
    }

    @Override
    public void addDebugScreenInfo(List<String> strings, RandomState randomState, BlockPos blockPos) {
        DecimalFormat decimalformat = new DecimalFormat("0.000");
        NoiseRouter noiserouter = randomState.router();
        DensityFunction.SinglePointContext densityfunction$singlepointcontext = new DensityFunction.SinglePointContext(blockPos.getX(), blockPos.getY(), blockPos.getZ());
        double d0 = noiserouter.ridges().compute(densityfunction$singlepointcontext);
        strings.add("NoiseRouter T: " + decimalformat.format(noiserouter.temperature().compute(densityfunction$singlepointcontext)) + " V: " + decimalformat.format(noiserouter.vegetation().compute(densityfunction$singlepointcontext)) + " C: " + decimalformat.format(noiserouter.continents().compute(densityfunction$singlepointcontext)) + " E: " + decimalformat.format(noiserouter.erosion().compute(densityfunction$singlepointcontext)) + " D: " + decimalformat.format(noiserouter.depth().compute(densityfunction$singlepointcontext)) + " W: " + decimalformat.format(d0) + " PV: " + decimalformat.format(NoiseRouterData.peaksAndValleys((float)d0)) + " AS: " + decimalformat.format(noiserouter.initialDensityWithoutJaggedness().compute(densityfunction$singlepointcontext)) + " N: " + decimalformat.format(noiserouter.finalDensity().compute(densityfunction$singlepointcontext)));
    }

    protected OptionalInt iterateNoiseColumn(LevelHeightAccessor levelHeightAccessor, RandomState randomState, int x, int z, MutableObject<NoiseColumn> mutableObject, Predicate<BlockState> blockStatePredicate) {
        NoiseSettings noisesettings = this.settings.value().noiseSettings().clampToHeightAccessor(levelHeightAccessor);
        int i = noisesettings.getCellHeight();
        int j = noisesettings.minY();
        int k = Mth.floorDiv(j, i);
        int l = Mth.floorDiv(noisesettings.height(), i);
        if (l > 0) {
            BlockState[] ablockstate;
            if (mutableObject == null) {
                ablockstate = null;
            }
            else {
                ablockstate = new BlockState[noisesettings.height()];
                mutableObject.setValue(new NoiseColumn(j, ablockstate));
            }

            int i1 = noisesettings.getCellWidth();
            int j1 = Math.floorDiv(x, i1);
            int k1 = Math.floorDiv(z, i1);
            int l1 = Math.floorMod(x, i1);
            int i2 = Math.floorMod(z, i1);
            int j2 = j1 * i1;
            int k2 = k1 * i1;
            double d0 = (double) l1 / (double) i1;
            double d1 = (double) i2 / (double) i1;
            NoiseChunk noiseChunk = new NoiseChunk(1, randomState, j2, k2, noisesettings, DensityFunctions.BeardifierMarker.INSTANCE, this.settings.value(), this.globalFluidPicker, Blender.empty());
            noiseChunk.initializeForFirstCellX();
            noiseChunk.advanceCellX(0);

            for (int l2 = l - 1; l2 >= 0; --l2) {
                noiseChunk.selectCellYZ(l2, 0);

                for (int i3 = i - 1; i3 >= 0; --i3) {
                    int j3 = (k + l2) * i + i3;
                    double d2 = (double) i3 / (double) i;
                    noiseChunk.updateForY(j3, d2);
                    noiseChunk.updateForX(x, d0);
                    noiseChunk.updateForZ(z, d1);
                    BlockState blockstate = ((NoiseChunkAccessor) noiseChunk).callGetInterpolatedState();
                    BlockState blockstate1 = blockstate == null ? this.defaultBlock : blockstate;
                    if (ablockstate != null) {
                        int k3 = l2 * i + i3;
                        ablockstate[k3] = blockstate1;
                    }

                    if (blockStatePredicate != null && blockStatePredicate.test(blockstate1)) {
                        noiseChunk.stopInterpolation();
                        return OptionalInt.of(j3 + 1);
                    }
                }
            }

            noiseChunk.stopInterpolation();
        }
        return OptionalInt.empty();
    }

    @Override
    public void buildSurface(WorldGenRegion worldGenRegion, StructureManager structureManager, RandomState randomState, ChunkAccess chunkAccess) {
        WorldGenerationContext worldgenerationcontext = new WorldGenerationContext(this, worldGenRegion);
        BiomeManager biomeManager;
        if (biomeSource instanceof BiomeManager.NoiseBiomeSource noiseBiomeSource) {
            biomeManager = new BiomeManager(noiseBiomeSource, worldGenRegion.getSeed());
        }
        else {
            biomeManager = worldGenRegion.getBiomeManager();
        }

        biomeManager = new NoVerticalBlendBiomeManager(biomeManager);
        this.buildSurface(chunkAccess, worldgenerationcontext, randomState, structureManager, biomeManager, worldGenRegion.registryAccess().registry(Registries.BIOME).get(), Blender.of(worldGenRegion));
    }

    public void buildSurface(ChunkAccess chunkAccess, WorldGenerationContext worldGenerationContext, RandomState randomState, StructureManager structureManager, BiomeManager biomeManager, Registry<Biome> biomeRegistry, Blender blender) {
        NoiseChunk noisechunk = chunkAccess.getOrCreateNoiseChunk((noiseChunk) -> this.createNoiseChunk(noiseChunk, structureManager, blender, randomState));
        NoiseGeneratorSettings noisegeneratorsettings = this.settings.value();
        randomState.surfaceSystem().buildSurface(randomState, biomeManager, biomeRegistry, noisegeneratorsettings.useLegacyRandomSource(), worldGenerationContext, chunkAccess, noisechunk, noisegeneratorsettings.surfaceRule());
    }

    @Override
    public void applyCarvers(WorldGenRegion worldGenRegion, long seed, RandomState randomState, BiomeManager biomeManager, StructureManager structureManager, ChunkAccess chunkAccess, GenerationStep.Carving carving) {}

    @Override
    protected ChunkAccess doFill(Blender blender, StructureManager structureManager, RandomState randomState, ChunkAccess chunkAccess, int x, int z) {
        NoiseChunk noiseChunk = chunkAccess.getOrCreateNoiseChunk((chunkAccess1) -> this.createNoiseChunk(chunkAccess1, structureManager, blender, randomState));
        Heightmap heightmap = chunkAccess.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap heightmap1 = chunkAccess.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        ChunkPos chunkpos = chunkAccess.getPos();
        int i = chunkpos.getMinBlockX();
        int j = chunkpos.getMinBlockZ();
        Aquifer aquifer = noiseChunk.aquifer();
        noiseChunk.initializeForFirstCellX();
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
        int k = this.settings.value().noiseSettings().getCellWidth();
        int l = this.settings.value().noiseSettings().getCellHeight();
        int i1 = 16 / k;
        int j1 = 16 / k;

        for(int k1 = 0; k1 < i1; ++k1) {
            noiseChunk.advanceCellX(k1);

            for(int l1 = 0; l1 < j1; ++l1) {
                int bottomBlockY = chunkAccess.getSectionsCount() - 1;
                LevelChunkSection levelchunksection = chunkAccess.getSection(chunkAccess.getSectionsCount() - 1);

                for(int i2 = z - 1; i2 >= 0; --i2) {
                    noiseChunk.selectCellYZ(i2, l1);

                    for(int j2 = l - 1; j2 >= 0; --j2) {
                        int k2 = (x + i2) * l + j2;
                        int l2 = k2 & 15;
                        int i3 = chunkAccess.getSectionIndex(k2);
                        if (bottomBlockY != i3) {
                            levelchunksection = chunkAccess.getSection(i3);
                        }

                        double d0 = (double)j2 / (double)l;
                        noiseChunk.updateForY(k2, d0);

                        for(int j3 = 0; j3 < k; ++j3) {
                            int k3 = i + k1 * k + j3;
                            int l3 = k3 & 15;
                            double d1 = (double)j3 / (double)k;
                            noiseChunk.updateForX(k3, d1);

                            for(int i4 = 0; i4 < k; ++i4) {
                                int j4 = j + l1 * k + i4;
                                int k4 = j4 & 15;
                                double d2 = (double)i4 / (double)k;
                                noiseChunk.updateForZ(j4, d2);
                                BlockState blockstate = ((NoiseChunkAccessor)noiseChunk).callGetInterpolatedState();
                                if (blockstate == null) {
                                    blockstate = this.defaultBlock;
                                }

                                if (blockstate != Blocks.AIR.defaultBlockState()) {
                                    levelchunksection.setBlockState(l3, l2, k4, blockstate, false);
                                    heightmap.update(l3, k2, k4, blockstate);
                                    heightmap1.update(l3, k2, k4, blockstate);
                                    if (aquifer.shouldScheduleFluidUpdate() && !blockstate.getFluidState().isEmpty()) {
                                        blockpos$mutableblockpos.set(k3, k2, j4);
                                        chunkAccess.markPosForPostprocessing(blockpos$mutableblockpos);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            noiseChunk.swapSlices();
        }

        noiseChunk.stopInterpolation();
        return chunkAccess;
    }

    @Override
    public int getGenDepth() {
        return this.settings.value().noiseSettings().height();
    }

    @Override
    public int getSeaLevel() {
        return this.settings.value().seaLevel();
    }

    @Override
    public int getMinY() {
        return this.settings.value().noiseSettings().minY();
    }

    @Override
    public Holder<NoiseGeneratorSettings> generatorSettings() {
        return this.settings;
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
        if (!this.settings.value().disableMobGeneration()) {
            ChunkPos chunkpos = region.getCenter();
            Holder<Biome> holder = region.getBiome(chunkpos.getWorldPosition().atY(region.getMaxBuildHeight() - 1));
            WorldgenRandom worldgenrandom = new WorldgenRandom(new LegacyRandomSource(RandomSupport.generateUniqueSeed()));
            worldgenrandom.setDecorationSeed(region.getSeed(), chunkpos.getMinBlockX(), chunkpos.getMinBlockZ());
            spawnNonBeeMobsForChunkGeneration(region, holder, chunkpos, worldgenrandom);
        }
    }

    public static void spawnNonBeeMobsForChunkGeneration(ServerLevelAccessor serverLevelAccessor, Holder<Biome> biomeHolder, ChunkPos chunkPos, RandomSource randomSource) {
        MobSpawnSettings mobspawnsettings = biomeHolder.value().getMobSettings();
        WeightedRandomList<MobSpawnSettings.SpawnerData> weightedrandomlist = mobspawnsettings.getMobs(MobCategory.CREATURE);
        weightedrandomlist = WeightedRandomList.create(weightedrandomlist.unwrap().stream().filter(e -> e.type != EntityType.BEE).toList());
        if (!weightedrandomlist.isEmpty()) {
            int i = chunkPos.getMinBlockX();
            int j = chunkPos.getMinBlockZ();
            int seaLevel = ((ServerChunkCache)serverLevelAccessor.getChunkSource()).getGenerator().getSeaLevel();

            while(randomSource.nextFloat() < mobspawnsettings.getCreatureProbability() * 0.5) {
                Optional<MobSpawnSettings.SpawnerData> optional = weightedrandomlist.getRandom(randomSource);
                if (optional.isPresent()) {
                    MobSpawnSettings.SpawnerData mobspawnsettings$spawnerdata = optional.get();
                    int k = mobspawnsettings$spawnerdata.minCount + randomSource.nextInt(1 + mobspawnsettings$spawnerdata.maxCount - mobspawnsettings$spawnerdata.minCount);
                    SpawnGroupData spawngroupdata = null;
                    int x = i + randomSource.nextInt(14) + 1;
                    int z = j + randomSource.nextInt(14) + 1;
                    int j1 = x;
                    int k1 = z;

                    for(int l1 = 0; l1 < k; ++l1) {
                        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos(x, randomSource.nextInt(250 - seaLevel) + seaLevel, z);
                        if (serverLevelAccessor.dimensionType().hasCeiling()) {
                            do {
                                mutableBlockPos.move(Direction.DOWN);
                            } while (!serverLevelAccessor.getBlockState(mutableBlockPos).isAir());

                            do {
                                mutableBlockPos.move(Direction.DOWN);
                            } while (serverLevelAccessor.getBlockState(mutableBlockPos).isAir() && mutableBlockPos.getY() > serverLevelAccessor.getMinBuildHeight());
                        }

                        if (mobspawnsettings$spawnerdata.type.canSummon()) {
                            float mobWidth = mobspawnsettings$spawnerdata.type.getWidth();
                            double finalX = Mth.clamp(x, (double)i + (double)mobWidth, (double)i + 16.0D - (double)mobWidth) + 0.5D;
                            double finalZ = Mth.clamp(z, (double)j + (double)mobWidth, (double)j + 16.0D - (double)mobWidth) + 0.5D;

                            if (!serverLevelAccessor.getWorldBorder().isWithinBounds(finalX, finalZ) ||
                                (mutableBlockPos.getY() < serverLevelAccessor.getMinBuildHeight() || mutableBlockPos.getY() >= serverLevelAccessor.getMaxBuildHeight()))
                            {
                                continue;
                            }

                            Entity entity = null;
                            try {
                                entity = mobspawnsettings$spawnerdata.type.create(serverLevelAccessor.getLevel());

                                entity.moveTo(finalX, mutableBlockPos.getY(), finalZ, randomSource.nextFloat() * 360.0F, 0.0F);
                                if (entity instanceof Mob mob) {
                                    PlatformHooks.finalizeSpawn(mob, serverLevelAccessor, null, MobSpawnType.CHUNK_GENERATION);

                                    if (mob.checkSpawnObstruction(serverLevelAccessor)) {
                                        spawngroupdata = mob.finalizeSpawn(serverLevelAccessor, serverLevelAccessor.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.CHUNK_GENERATION, spawngroupdata);
                                        mob.moveTo(mob.getX(), mob.getY() + 1, mob.getZ());
                                        serverLevelAccessor.addFreshEntityWithPassengers(mob);
                                    }
                                }
                            }
                            catch (Exception exception) {
                                Bumblezone.LOGGER.error("Failed to create mob: {}", entity);
                                exception.addSuppressed(new RuntimeException("Failed to create mob: " + entity));
                                throw exception;
                            }
                        }

                        x += randomSource.nextInt(5) - randomSource.nextInt(5);

                        for(z += randomSource.nextInt(5) - randomSource.nextInt(5); x < i || x >= i + 16 || z < j || z >= j + 16; z = k1 + randomSource.nextInt(5) - randomSource.nextInt(5)) {
                            x = j1 + randomSource.nextInt(5) - randomSource.nextInt(5);
                        }
                    }
                }
            }
        }
    }
}