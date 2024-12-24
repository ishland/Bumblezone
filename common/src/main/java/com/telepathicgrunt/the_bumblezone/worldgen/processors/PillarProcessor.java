package com.telepathicgrunt.the_bumblezone.worldgen.processors;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.telepathicgrunt.the_bumblezone.modinit.BzProcessors;
import com.telepathicgrunt.the_bumblezone.utils.GeneralUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class PillarProcessor extends StructureProcessor {
    private static final ResourceLocation EMPTY_RL = ResourceLocation.fromNamespaceAndPath("minecraft", "empty");

    public static final MapCodec<PillarProcessor> CODEC  = RecordCodecBuilder.mapCodec((instance) -> instance.group(
            Codec.mapPair(BlockState.CODEC.fieldOf("trigger"), BlockState.CODEC.fieldOf("replacement"))
                    .codec().listOf()
                    .xmap((list) -> list.stream().collect(Collectors.toMap(Pair::getFirst, Pair::getSecond)),
                            (map) -> map.entrySet().stream().map((entry) -> Pair.of(entry.getKey(), entry.getValue())).collect(Collectors.toList()))
                    .fieldOf("pillar_trigger_and_replacements")
                    .forGetter((processor) -> processor.pillarTriggerAndReplacementBlocks),
            ResourceLocation.CODEC.optionalFieldOf("pillar_processor_list", EMPTY_RL).forGetter(processor -> processor.processorList),
            Direction.CODEC.optionalFieldOf("direction", Direction.DOWN).forGetter(processor -> processor.direction),
            IntProvider.codec(0, 1000).optionalFieldOf("pillar_length").forGetter(config -> config.pillarLength),
            Codec.BOOL.optionalFieldOf("forced_placement", false).forGetter(config -> config.forcePlacement))
    .apply(instance, instance.stable(PillarProcessor::new)));

    public final Map<BlockState, BlockState> pillarTriggerAndReplacementBlocks;
    public final ResourceLocation processorList;
    public final Direction direction;
    public final Optional<IntProvider> pillarLength;
    public final boolean forcePlacement;

    private PillarProcessor(Map<BlockState, BlockState> pillarTriggerAndReplacementBlocks,
                            ResourceLocation processorList,
                            Direction direction,
                            Optional<IntProvider> pillarLength,
                            boolean forcePlacement)
    {
        this.pillarTriggerAndReplacementBlocks = pillarTriggerAndReplacementBlocks;
        this.processorList = processorList;
        this.direction = direction;
        this.pillarLength = pillarLength;
        this.forcePlacement = forcePlacement;
    }

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader levelReader, BlockPos templateOffset, BlockPos worldOffset, StructureTemplate.StructureBlockInfo structureBlockInfoLocal, StructureTemplate.StructureBlockInfo structureBlockInfoWorld, StructurePlaceSettings structurePlacementData) {
        if (GeneralUtils.isOutsideCenterWorldgenRegionChunk(levelReader, structureBlockInfoWorld.pos())) {
            return structureBlockInfoWorld;
        }

        BlockState blockState = structureBlockInfoWorld.state();
        if (pillarTriggerAndReplacementBlocks.containsKey(blockState)) {
            BlockPos worldPos = structureBlockInfoWorld.pos();
            RandomSource random = RandomSource.create();
            random.setSeed(worldPos.asLong());
            boolean isNotInWorldgen = levelReader instanceof ServerLevel;

            BlockState replacementState = pillarTriggerAndReplacementBlocks.get(blockState);
            BlockPos.MutableBlockPos currentPos = new BlockPos.MutableBlockPos().set(worldPos);
            StructureProcessorList structureProcessorList = null;
            if(processorList != null && !processorList.equals(EMPTY_RL)) {
                structureProcessorList = levelReader.registryAccess().registryOrThrow(Registries.PROCESSOR_LIST).get(processorList);
            }

            if(levelReader instanceof WorldGenRegion worldGenRegion && !worldGenRegion.getCenter().equals(new ChunkPos(currentPos))) {
                return replacementState == null || replacementState.is(Blocks.STRUCTURE_VOID) ? null : new StructureTemplate.StructureBlockInfo(worldPos, replacementState, null);
            }

            int pillarHeight = pillarLength.map(intProvider -> intProvider.sample(random)).orElse(1000);
            int terrainY = Integer.MIN_VALUE;
            if(direction == Direction.DOWN && !forcePlacement) {
                terrainY = GeneralUtils.getFirstLandYFromPos(levelReader, worldPos);
                if(terrainY <= levelReader.getMinBuildHeight() && pillarHeight + 2 >= worldPos.getY() - levelReader.getMinBuildHeight()) {
                    // Replaces the data block itself
                    return (replacementState == null || replacementState.is(Blocks.STRUCTURE_VOID)) ?
                            null : new StructureTemplate.StructureBlockInfo(worldPos, replacementState, null);
                }
            }

            boolean isVertical = direction.getAxis() == Direction.Axis.Y;
            ChunkAccess currentChunk = levelReader.getChunk(worldPos);

            // Creates the pillars in the world that replaces air and liquids
            BlockState currentBlock = currentChunk.getBlockState(worldPos.below());
            while((forcePlacement || (!currentBlock.canOcclude() && !currentBlock.hasBlockEntity()) || currentBlock.is(BlockTags.REPLACEABLE)) &&
                (forcePlacement || currentPos.getY() >= terrainY) &&
                !levelReader.isOutsideBuildHeight(currentPos.getY()) &&
                currentPos.closerThan(worldPos, pillarHeight)
            ) {
                StructureTemplate.StructureBlockInfo newPillarState1 = new StructureTemplate.StructureBlockInfo(currentPos.subtract(worldPos).offset(templateOffset), replacementState, null);
                StructureTemplate.StructureBlockInfo newPillarState2 = new StructureTemplate.StructureBlockInfo(currentPos.immutable(), replacementState, null);

                if(structureProcessorList != null) {
                    for(StructureProcessor processor : structureProcessorList.list()) {
                        if(newPillarState2 == null) {
                            break;
                        }
                        newPillarState2 = processor.processBlock(levelReader, newPillarState1.pos(), newPillarState2.pos(), newPillarState1, newPillarState2, structurePlacementData);
                    }
                }

                if(newPillarState2 != null) {
                    BlockState newState = newPillarState2.state().rotate(structurePlacementData.getRotation());
                    if (isNotInWorldgen) {
                        ((ServerLevel)levelReader).setBlock(currentPos, newState, 3);
                    }
                    else {
                        if (isVertical) {
                            currentChunk.setBlockState(currentPos, newState, false);
                        }
                        else {
                            levelReader.getChunk(currentPos).setBlockState(currentPos, newState, false);
                        }
                    }
                }

                currentPos.move(direction);
                currentBlock = levelReader.getBlockState(currentPos);
            }

            // Replaces the data block itself
            return (replacementState == null || replacementState.is(Blocks.STRUCTURE_VOID)) ?
                    null : new StructureTemplate.StructureBlockInfo(worldPos, replacementState, null);
        }

        return structureBlockInfoWorld;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return BzProcessors.PILLAR_PROCESSOR.get();
    }
}
