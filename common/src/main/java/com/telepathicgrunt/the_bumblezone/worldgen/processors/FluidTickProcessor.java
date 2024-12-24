package com.telepathicgrunt.the_bumblezone.worldgen.processors;

import com.mojang.serialization.MapCodec;
import com.telepathicgrunt.the_bumblezone.modinit.BzProcessors;
import com.telepathicgrunt.the_bumblezone.utils.GeneralUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class FluidTickProcessor extends StructureProcessor {

    public static final MapCodec<FluidTickProcessor> CODEC = MapCodec.unit(FluidTickProcessor::new);

    public FluidTickProcessor() { }


    @Override
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader levelReader, BlockPos pos, BlockPos blockPos, StructureTemplate.StructureBlockInfo structureBlockInfoLocal, StructureTemplate.StructureBlockInfo structureBlockInfoWorld, StructurePlaceSettings structurePlacementData) {
        if (GeneralUtils.isOutsideCenterWorldgenRegionChunk(levelReader, structureBlockInfoWorld.pos())) {
            return structureBlockInfoWorld;
        }

        BlockState structureState = structureBlockInfoWorld.state();
        if (!structureState.getFluidState().isEmpty() && structureBlockInfoWorld.pos().getY() > levelReader.getMinBuildHeight() && structureBlockInfoWorld.pos().getY() < levelReader.getMaxBuildHeight()) {
            ((LevelAccessor)levelReader).scheduleTick(structureBlockInfoWorld.pos(), structureState.getFluidState().getType(), 0);
        }

        return structureBlockInfoWorld;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return BzProcessors.FLUID_TICK_PROCESSOR.get();
    }
}