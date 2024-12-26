package com.telepathicgrunt.the_bumblezone.worldgen.processors;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.telepathicgrunt.the_bumblezone.blocks.EmptyHoneycombBrood;
import com.telepathicgrunt.the_bumblezone.blocks.HoneycombBrood;
import com.telepathicgrunt.the_bumblezone.blocks.PileOfPollen;
import com.telepathicgrunt.the_bumblezone.modinit.BzBlocks;
import com.telepathicgrunt.the_bumblezone.modinit.BzFluids;
import com.telepathicgrunt.the_bumblezone.modinit.BzProcessors;
import com.telepathicgrunt.the_bumblezone.utils.GeneralUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class HoneycombHoleProcessor extends StructureProcessor {

    public static final MapCodec<HoneycombHoleProcessor> CODEC = RecordCodecBuilder.mapCodec((instance) -> instance.group(
            Codec.INT.fieldOf("flood_level").forGetter(config -> config.floodLevel)
    ).apply(instance, instance.stable(HoneycombHoleProcessor::new)));

    private final int floodLevel;

    private HoneycombHoleProcessor(int floodLevel) { this.floodLevel = floodLevel; }

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader levelReader, BlockPos pos, BlockPos blockPos, StructureTemplate.StructureBlockInfo structureBlockInfoLocal, StructureTemplate.StructureBlockInfo structureBlockInfoWorld, StructurePlaceSettings settings) {
        BlockPos worldPos = structureBlockInfoWorld.pos();
        if (GeneralUtils.isOutsideStructureAllowedBounds(settings, worldPos)) {
            return structureBlockInfoWorld;
        }

        BlockState placingState = structureBlockInfoWorld.state();
        ChunkAccess chunk = levelReader.getChunk(structureBlockInfoWorld.pos());
        LevelChunkSection chunkSection = chunk.getSection(levelReader.getSectionIndex(worldPos.getY()));
        BlockState checkedState = getBlockStateFromSection(chunkSection, worldPos);

        // does world checks for cave and pollen powder
        if (checkedState.isAir() || !checkedState.getFluidState().isEmpty()) {
            if (placingState.isAir() || placingState.is(BzBlocks.PILE_OF_POLLEN.get())) {
                if (!checkedState.getFluidState().isEmpty() || worldPos.getY() <= floodLevel) {
                    setBlockStateFromSection(chunkSection, worldPos, BzFluids.SUGAR_WATER_BLOCK.get().defaultBlockState());
                    if (checkedState.hasBlockEntity()) {
                        chunk.removeBlockEntity(worldPos);
                    }
                    return null;
                }
            }
            else {
                return null;
            }
        }

        // brood
        if (placingState.is(BzBlocks.HONEYCOMB_BROOD.get())) {
            if (checkedState.hasBlockEntity()) {
                chunk.removeBlockEntity(worldPos);
            }

            RandomSource random = settings.getRandom(worldPos);
            if (random.nextInt(5) < 2) {
                return new StructureTemplate.StructureBlockInfo(worldPos, placingState.setValue(HoneycombBrood.STAGE, random.nextInt(3)), null);
            }
            else if (random.nextInt(13) == 0) {
                return new StructureTemplate.StructureBlockInfo(
                        worldPos,
                        BzBlocks.EMPTY_HONEYCOMB_BROOD.get().defaultBlockState()
                                .setValue(EmptyHoneycombBrood.FACING, placingState.getValue(HoneycombBrood.FACING)),
                        null);
            }
            else if (random.nextInt(4) == 0) {
                return new StructureTemplate.StructureBlockInfo(worldPos, BzBlocks.FILLED_POROUS_HONEYCOMB.get().defaultBlockState(), null);
            }
            else {
                return new StructureTemplate.StructureBlockInfo(worldPos, Blocks.HONEY_BLOCK.defaultBlockState(), null);
            }
        }

        // ring around brood
        if (placingState.is(Blocks.HONEY_BLOCK) || placingState.is(BzBlocks.FILLED_POROUS_HONEYCOMB.get())) {
            if (checkedState.hasBlockEntity()) {
                chunk.removeBlockEntity(worldPos);
            }

            RandomSource random = settings.getRandom(worldPos);
            if (random.nextInt(3) == 0) {
                return new StructureTemplate.StructureBlockInfo(worldPos, BzBlocks.FILLED_POROUS_HONEYCOMB.get().defaultBlockState(), null);
            }
            else {
                return new StructureTemplate.StructureBlockInfo(worldPos, Blocks.HONEY_BLOCK.defaultBlockState(), null);
            }
        }

        // Pollen pile
        else if (placingState.is(BzBlocks.PILE_OF_POLLEN.get())) {
            // Check if pollen pile can even be placed here safely
            BlockState belowState = chunk.getBlockState(worldPos.below());
            if (belowState.isAir() || !belowState.getFluidState().isEmpty()) {
                return null;
            }

            if (checkedState.hasBlockEntity()) {
                chunk.removeBlockEntity(worldPos);
            }

            RandomSource random = settings.getRandom(worldPos);
            if (random.nextInt(80) != 0) {
                return new StructureTemplate.StructureBlockInfo(worldPos, Blocks.CAVE_AIR.defaultBlockState(), null);
            }
            else {
                return new StructureTemplate.StructureBlockInfo(worldPos, BzBlocks.PILE_OF_POLLEN.get().defaultBlockState().setValue(PileOfPollen.LAYERS, random.nextInt(3) + 1), null);
            }
        }

        // main body
        else if (placingState.is(Blocks.HONEYCOMB_BLOCK)) {
            RandomSource random = settings.getRandom(worldPos);
            if (random.nextInt(3) != 0) {
                if (checkedState.hasBlockEntity()) {
                    chunk.removeBlockEntity(worldPos);
                }

                return new StructureTemplate.StructureBlockInfo(worldPos, BzBlocks.FILLED_POROUS_HONEYCOMB.get().defaultBlockState(), null);
            }
        }

        if (checkedState.hasBlockEntity()) {
            chunk.removeBlockEntity(worldPos);
        }
        return structureBlockInfoWorld;
    }

    private static BlockState getBlockStateFromSection(LevelChunkSection chunkSection, BlockPos blockPos) {
        int i = SectionPos.sectionRelative(blockPos.getX());
        int j = SectionPos.sectionRelative(blockPos.getY());
        int k = SectionPos.sectionRelative(blockPos.getZ());
        return chunkSection.getBlockState(i, j, k);
    }

    private static void setBlockStateFromSection(LevelChunkSection chunkSection, BlockPos blockPos, BlockState newState) {
        int i = SectionPos.sectionRelative(blockPos.getX());
        int j = SectionPos.sectionRelative(blockPos.getY());
        int k = SectionPos.sectionRelative(blockPos.getZ());
        chunkSection.setBlockState(i, j, k, newState);
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return BzProcessors.HONEYCOMB_HOLE_PROCESSOR.get();
    }
}