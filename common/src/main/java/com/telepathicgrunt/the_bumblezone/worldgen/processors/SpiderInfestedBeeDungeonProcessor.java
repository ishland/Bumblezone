package com.telepathicgrunt.the_bumblezone.worldgen.processors;

import com.mojang.serialization.MapCodec;
import com.telepathicgrunt.the_bumblezone.blocks.HoneycombBrood;
import com.telepathicgrunt.the_bumblezone.configs.BzWorldgenConfigs;
import com.telepathicgrunt.the_bumblezone.modcompat.ModChecker;
import com.telepathicgrunt.the_bumblezone.modcompat.ModCompat;
import com.telepathicgrunt.the_bumblezone.modinit.BzBlocks;
import com.telepathicgrunt.the_bumblezone.modinit.BzFluids;
import com.telepathicgrunt.the_bumblezone.modinit.BzProcessors;
import com.telepathicgrunt.the_bumblezone.modinit.BzTags;
import com.telepathicgrunt.the_bumblezone.utils.GeneralUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.Optional;

/**
 * POOL ENTRY MUST BE USING legacy_single_pool_element OR ELSE THE STRUCTURE BLOCK IS REMOVED BEFORE THIS PROCESSOR RUNS.
 */
public class SpiderInfestedBeeDungeonProcessor extends StructureProcessor {

    public static final MapCodec<SpiderInfestedBeeDungeonProcessor> CODEC = MapCodec.unit(SpiderInfestedBeeDungeonProcessor::new);
    private SpiderInfestedBeeDungeonProcessor() { }

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader levelReader, BlockPos pos, BlockPos blockPos, StructureTemplate.StructureBlockInfo structureBlockInfoLocal, StructureTemplate.StructureBlockInfo structureBlockInfoWorld, StructurePlaceSettings settings) {
        if (GeneralUtils.isOutsideStructureAllowedBounds(settings, structureBlockInfoWorld.pos())) {
            return structureBlockInfoWorld;
        }

        BlockState blockState = structureBlockInfoWorld.state();
        BlockPos worldPos = structureBlockInfoWorld.pos();
        CompoundTag nbt = structureBlockInfoWorld.nbt();

        // placing altar blocks
        if (blockState.is(Blocks.STRUCTURE_BLOCK)) {
            CompoundTag compoundTag = structureBlockInfoWorld.nbt();
            if (compoundTag == null) {
                return structureBlockInfoWorld;
            }
            String metadata = compoundTag.getString("metadata");
            BlockState belowBlock = levelReader.getChunk(worldPos).getBlockState(worldPos);

            //altar blocks cannot be placed on air
            if(belowBlock.isAir()) {
                blockState = Blocks.CAVE_AIR.defaultBlockState();
            }
            else {
                RandomSource random = new WorldgenRandom(new LegacyRandomSource(worldPos.asLong() * worldPos.getY()));
                switch (metadata) {
                    case "center" -> {
                        if (random.nextFloat() < 0.1f) {
                            blockState = BzBlocks.HONEY_COCOON.get().defaultBlockState();
                            nbt = new CompoundTag();
                            nbt.putString("LootTable", "the_bumblezone:structures/spider_infested_bee_dungeon");
                        }
                        else if (random.nextFloat() < 0.6f) {
                            blockState = BzBlocks.HONEY_CRYSTAL.get().defaultBlockState();
                        }
                        else if (random.nextFloat() < 0.25f) {
                            Optional<HolderSet.Named<Block>> optionalBlocks = BuiltInRegistries.BLOCK.getTag(BzTags.SPIDER_INFESTED_BEE_DUNGEON_POSSIBLE_CANDLES);
                            if (optionalBlocks.isPresent()) {
                                blockState = optionalBlocks.get().get(random.nextInt(optionalBlocks.get().size())).value().defaultBlockState();
                                blockState = blockState.setValue(CandleBlock.CANDLES, random.nextInt(4) + 1);
                                blockState = blockState.setValue(CandleBlock.LIT, false);
                            }
                        }
                        else if (random.nextFloat() < 0.05f) {
                            blockState = Blocks.COBWEB.defaultBlockState();
                        }
                        else {
                            blockState = Blocks.CAVE_AIR.defaultBlockState();
                        }
                    }
                    case "inner_ring" -> {
                        if (random.nextFloat() < 0.3f) {
                            blockState = BzBlocks.HONEY_CRYSTAL.get().defaultBlockState();
                        }
                        else if (random.nextFloat() < 0.07f) {
                            blockState = Blocks.COBWEB.defaultBlockState();
                        }
                        else {
                            blockState = Blocks.CAVE_AIR.defaultBlockState();
                        }
                    }
                    case "outer_ring" -> {
                        if (random.nextFloat() < 0.4f) {
                            blockState = BzBlocks.HONEY_CRYSTAL.get().defaultBlockState();
                        }
                        else if (random.nextFloat() < 0.2f) {
                            Optional<HolderSet.Named<Block>> optionalBlocks = BuiltInRegistries.BLOCK.getTag(BzTags.SPIDER_INFESTED_BEE_DUNGEON_POSSIBLE_CANDLES);
                            if (optionalBlocks.isPresent()) {
                                blockState = optionalBlocks.get().get(random.nextInt(optionalBlocks.get().size())).value().defaultBlockState();
                                blockState = blockState.setValue(CandleBlock.CANDLES, random.nextInt(random.nextInt(4) + 1) + 1);
                                blockState = blockState.setValue(CandleBlock.LIT, false);
                            }
                        }
                        else if (random.nextFloat() < 0.07f) {
                            blockState = Blocks.COBWEB.defaultBlockState();
                        }
                        else {
                            blockState = Blocks.CAVE_AIR.defaultBlockState();
                        }
                    }
                    default -> {
                    }
                }
            }
        }

        // main body and ceiling
        else if(blockState.is(Blocks.HONEYCOMB_BLOCK) || blockState.is(BzBlocks.FILLED_POROUS_HONEYCOMB.get())) {

            boolean compatSuccess = false;
            RandomSource random = new WorldgenRandom(new LegacyRandomSource(worldPos.asLong() * worldPos.getY()));

            for (ModCompat compat : ModChecker.DUNGEON_COMB_COMPATS) {
                if (compat.checkCombSpawn(worldPos, random, levelReader, true)) {
                    StructureTemplate.StructureBlockInfo info = compat.getHoneycomb(worldPos, random, levelReader, true);
                    if (info != null) {
                        return info;
                    }
                }
            }

            if (!compatSuccess) {
                if (random.nextFloat() < 0.15f) {
                    blockState = Blocks.HONEYCOMB_BLOCK.defaultBlockState();
                }
                else {
                    blockState = BzBlocks.POROUS_HONEYCOMB.get().defaultBlockState();
                }
            }
        }

        // walls
        else if(blockState.is(BzBlocks.HONEYCOMB_BROOD.get())) {
            RandomSource random = new WorldgenRandom(new LegacyRandomSource(worldPos.asLong() * worldPos.getY()));
            if (random.nextFloat() < 0.6f) {
                blockState = BzBlocks.EMPTY_HONEYCOMB_BROOD.get().defaultBlockState()
                        .setValue(HoneycombBrood.FACING, blockState.getValue(HoneycombBrood.FACING));
            }
            else if (random.nextDouble() < BzWorldgenConfigs.spawnerRateSpiderBeeDungeon) {
                blockState = Blocks.SPAWNER.defaultBlockState();
            }
            else {
                blockState = BzBlocks.POROUS_HONEYCOMB.get().defaultBlockState();
            }
        }

        // sugar water
        else if(blockState.is(BzFluids.SUGAR_WATER_BLOCK.get())) {
            blockState = Blocks.CAVE_AIR.defaultBlockState();
        }

        return new StructureTemplate.StructureBlockInfo(worldPos, blockState, nbt);
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return BzProcessors.SPIDER_INFESTED_BEE_DUNGEON_PROCESSOR.get();
    }
}