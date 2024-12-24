package com.telepathicgrunt.the_bumblezone.worldgen.processors;

import com.mojang.serialization.MapCodec;
import com.telepathicgrunt.the_bumblezone.modinit.BzProcessors;
import com.telepathicgrunt.the_bumblezone.utils.GeneralUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class TickFluidProcessor extends StructureProcessor {

    public static final MapCodec<TickFluidProcessor> CODEC = MapCodec.unit(TickFluidProcessor::new);

    private TickFluidProcessor() { }

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader levelReader, BlockPos pos, BlockPos pos2, StructureTemplate.StructureBlockInfo infoIn1, StructureTemplate.StructureBlockInfo structureBlockInfoWorld, StructurePlaceSettings settings) {
        if (!structureBlockInfoWorld.state().getFluidState().isEmpty()) {
            if (GeneralUtils.isOutsideStructureAllowedBounds(settings, structureBlockInfoWorld.pos())) {
                return structureBlockInfoWorld;
            }

            ChunkAccess chunk = levelReader.getChunk(structureBlockInfoWorld.pos());
            int minY = chunk.getMinBuildHeight();
            int maxY = chunk.getMaxBuildHeight();
            int currentY = structureBlockInfoWorld.pos().getY();
            if (currentY >= minY && currentY <= maxY) {
                ((LevelAccessor) levelReader).scheduleTick(structureBlockInfoWorld.pos(), structureBlockInfoWorld.state().getBlock(), 0);
            }
        }
        return structureBlockInfoWorld;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return BzProcessors.TICK_FLUID_PROCESSOR.get();
    }
}
