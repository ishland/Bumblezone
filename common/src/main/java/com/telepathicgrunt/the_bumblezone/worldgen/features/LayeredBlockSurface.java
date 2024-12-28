package com.telepathicgrunt.the_bumblezone.worldgen.features;

import com.mojang.serialization.Codec;
import com.telepathicgrunt.the_bumblezone.utils.OpenSimplex2F;
import com.telepathicgrunt.the_bumblezone.utils.UnsafeBulkSectionAccess;
import com.telepathicgrunt.the_bumblezone.worldgen.features.configs.BiomeBasedLayerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

import java.util.Optional;


public class LayeredBlockSurface extends Feature<BiomeBasedLayerConfig> {

    protected long seed;
    protected static OpenSimplex2F noiseGen;

    public void setSeed(long seed) {
        if (this.seed != seed || noiseGen == null) {
            noiseGen = new OpenSimplex2F(seed);
            this.seed = seed;
        }
    }

    public LayeredBlockSurface(Codec<BiomeBasedLayerConfig> configFactory) {
        super(configFactory);
    }

    @Override
    public boolean place(FeaturePlaceContext<BiomeBasedLayerConfig> context) {
        setSeed(context.level().getSeed());
        BlockPos.MutableBlockPos mutableBlockPos = context.origin().mutable();
        BlockPos.MutableBlockPos mutableBlockPosForChunk = new BlockPos.MutableBlockPos();
        ChunkPos chunkPos = new ChunkPos(mutableBlockPos);
        Biome targetBiome = context.level().registryAccess().registry(Registries.BIOME).get().get(context.config().biomeRL);

        UnsafeBulkSectionAccess bulkSectionAccess = new UnsafeBulkSectionAccess(context.level());
        for (int xOffset = -1; xOffset <= 1; xOffset++) {
            for (int zOffset = -1; zOffset <= 1; zOffset++) {
                ChunkPos currentChunkPos = new ChunkPos(chunkPos.x + xOffset, chunkPos.z + zOffset);
                mutableBlockPosForChunk.set(currentChunkPos.getWorldPosition());
                ChunkAccess cachedChunk = context.level().getChunk(currentChunkPos.getWorldPosition());

                if (xOffset != 0 && zOffset != 0 && cachedChunk.getSection(0).getBiomes().maybeHas((biome) -> biome.value() == targetBiome)) {
                    continue;
                }

               fillChunkWithPollen(context, bulkSectionAccess, cachedChunk, currentChunkPos.getWorldPosition(), targetBiome);
            }
        }
        return true;
    }

    private void fillChunkWithPollen(FeaturePlaceContext<BiomeBasedLayerConfig> context, UnsafeBulkSectionAccess bulkSectionAccess, ChunkAccess cachedChunk, BlockPos startPos, Biome targetBiome) {
        int configHeight = context.config().height;
        BlockState configBlockState = context.config().state;
        Optional<BlockState> configRareBlockState = context.config().rareState;
        BlockPos.MutableBlockPos mutable = context.origin().mutable();
        BlockState currentBlockState;
        BlockState previousBlockState = Blocks.AIR.defaultBlockState();
        RandomSource random = context.random();

        int maxY = (context.chunkGenerator().getGenDepth() + context.chunkGenerator().getMinY()) - 1;
        for (int xOffset = 0; xOffset <= 15; xOffset++) {
            for (int zOffset = 0; zOffset <= 15; zOffset++) {
                mutable.set(startPos.getX() + xOffset, maxY, startPos.getZ() + zOffset);
                if (targetBiome != context.level().getBiomeManager().getNoiseBiomeAtPosition(mutable).value()) {
                    continue;
                }

                while (mutable.getY() >= context.chunkGenerator().getSeaLevel() - 1) {
                    currentBlockState = bulkSectionAccess.getBlockState(mutable);

                    if (!currentBlockState.isAir() && currentBlockState.getFluidState().isEmpty() &&
                        !currentBlockState.is(configBlockState.getBlock()) && previousBlockState.getBlock() == Blocks.AIR &&
                        !(configRareBlockState.isPresent() && currentBlockState.is(configRareBlockState.get().getBlock())))
                    {
                        BlockState belowBlockState = bulkSectionAccess.getBlockState(mutable);
                        if (!belowBlockState.isFaceSturdy(context.level(), mutable, Direction.UP)) {
                            previousBlockState = currentBlockState;
                            mutable.move(Direction.DOWN);
                            continue;
                        }

                        for (int height = 1; height <= configHeight && mutable.getY() + height < maxY; height++) {
                            BlockPos finalPosition = mutable.above(height);
                            BlockState finalBlockState = bulkSectionAccess.getBlockState(finalPosition);
                            if (!finalBlockState.isAir()) {
                                break;
                            }

                            int layerHeight = 8;
                            if (height == configHeight) {
                                float xzScale = 0.035f;
                                float yScale = 0.015f;
                                double noiseVal = Math.abs(noiseGen.noise3_Classic(mutable.getX() * xzScale, (mutable.getY() * yScale) + height, mutable.getZ() * xzScale));
                                layerHeight = Math.max(1, (int) (((noiseVal * 0.63D) + 0.4D) * 8D));
                                layerHeight = Math.min(8, layerHeight);
                            }

                            BlockState blockToPlace = configBlockState;
                            if ((height < configHeight || layerHeight == 8) &&
                                configRareBlockState.isPresent() &&
                                random.nextFloat() < context.config().rareStateChance)
                            {
                                blockToPlace = configRareBlockState.get();
                            }
                            
                            // Vary the pollen piles
                            if (blockToPlace.hasProperty(BlockStateProperties.LAYERS)) {
                                bulkSectionAccess.setBlockState(finalPosition, blockToPlace.setValue(BlockStateProperties.LAYERS, layerHeight), false);
                                context.level().scheduleTick(finalPosition, blockToPlace.getBlock(), 0);
                            }
                            else {
                                bulkSectionAccess.setBlockState(finalPosition, blockToPlace, false);

                                if (blockToPlace.hasBlockEntity()) {
                                    BlockEntity blockEntity = ((EntityBlock)blockToPlace.getBlock()).newBlockEntity(finalPosition, blockToPlace);
                                    if (blockEntity != null) {
                                        if (blockEntity instanceof BrushableBlockEntity brushableBlock && context.config().suspiciousBlockLoot.isPresent()) {
                                            brushableBlock.setLootTable(ResourceKey.create(Registries.LOOT_TABLE, context.config().suspiciousBlockLoot.get()), random.nextLong());
                                        }
                                        cachedChunk.setBlockEntity(blockEntity);
                                    }
                                }
                            }
                        }
                    }

                    previousBlockState = currentBlockState;
                    mutable.move(Direction.DOWN);
                }
            }
        }
    }
}