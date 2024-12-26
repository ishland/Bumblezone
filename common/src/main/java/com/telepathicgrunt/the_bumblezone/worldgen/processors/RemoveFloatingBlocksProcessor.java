package com.telepathicgrunt.the_bumblezone.worldgen.processors;

import com.mojang.serialization.Codec;
import com.telepathicgrunt.the_bumblezone.modinit.BzProcessors;
import com.telepathicgrunt.the_bumblezone.utils.GeneralUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * For removing stuff like floating tall grass or kelp
 */
public class RemoveFloatingBlocksProcessor extends StructureProcessor {

    public static final Codec<RemoveFloatingBlocksProcessor> CODEC = Codec.unit(RemoveFloatingBlocksProcessor::new);
    private RemoveFloatingBlocksProcessor() { }

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader levelReader, BlockPos pos, BlockPos blockPos, StructureTemplate.StructureBlockInfo structureBlockInfoLocal, StructureTemplate.StructureBlockInfo structureBlockInfoWorld, StructurePlaceSettings settings) {
        if (GeneralUtils.isOutsideStructureAllowedBounds(settings, structureBlockInfoWorld.pos())) {
            return structureBlockInfoWorld;
        }

        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos().set(structureBlockInfoWorld.pos());
        ChunkAccess cachedChunk = levelReader.getChunk(mutable);

        // attempts to remove invalid floating plants
        if (structureBlockInfoWorld.state().isAir() || !structureBlockInfoWorld.state().getFluidState().isEmpty()) {

            // set the block in the world so that canPlaceAt's result changes
            cachedChunk.setBlockState(mutable, structureBlockInfoWorld.state(), false);
            BlockState aboveWorldState = levelReader.getBlockState(mutable.move(Direction.UP));

            // detects the invalidly placed blocks
            while (mutable.getY() < levelReader.getHeight() && !aboveWorldState.canSurvive(levelReader, mutable)) {
                cachedChunk.setBlockState(mutable, structureBlockInfoWorld.state(), false);
                mutable.move(Direction.UP);
                aboveWorldState = levelReader.getBlockState(mutable);
            }
        }
        else if (!structureBlockInfoWorld.state().canSurvive(levelReader, mutable)) {
            return new StructureTemplate.StructureBlockInfo(structureBlockInfoWorld.pos(), Blocks.CAVE_AIR.defaultBlockState(), null);
        }

        return structureBlockInfoWorld;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return BzProcessors.REMOVE_FLOATING_BLOCKS_PROCESSOR.get();
    }
}
