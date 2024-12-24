package com.telepathicgrunt.the_bumblezone.worldgen.processors;

import com.mojang.serialization.MapCodec;
import com.telepathicgrunt.the_bumblezone.blocks.HoneycombBrood;
import com.telepathicgrunt.the_bumblezone.modinit.BzBlocks;
import com.telepathicgrunt.the_bumblezone.modinit.BzProcessors;
import com.telepathicgrunt.the_bumblezone.utils.GeneralUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * POOL ENTRY MUST BE USING legacy_single_pool_element OR ELSE THE STRUCTURE BLOCK IS REMOVED BEFORE THIS PROCESSOR RUNS.
 */
public class HoneycombBroodRandomizeProcessor extends StructureProcessor {

    public static final MapCodec<HoneycombBroodRandomizeProcessor> CODEC = MapCodec.unit(HoneycombBroodRandomizeProcessor::new);

    private HoneycombBroodRandomizeProcessor() {
    }

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader levelReader, BlockPos pos, BlockPos blockPos, StructureTemplate.StructureBlockInfo structureBlockInfoLocal, StructureTemplate.StructureBlockInfo structureBlockInfoWorld, StructurePlaceSettings settings) {
        if (structureBlockInfoWorld.state().is(BzBlocks.HONEYCOMB_BROOD.get())) {
            if (GeneralUtils.isOutsideStructureAllowedBounds(settings, structureBlockInfoWorld.pos())) {
                return structureBlockInfoWorld;
            }

            BlockState blockState = structureBlockInfoWorld.state();
            BlockPos worldPos = structureBlockInfoWorld.pos();
            CompoundTag nbt = structureBlockInfoWorld.nbt();

            return new StructureTemplate.StructureBlockInfo(
                    worldPos,
                    BzBlocks.HONEYCOMB_BROOD.get().defaultBlockState()
                        .setValue(HoneycombBrood.STAGE, settings.getRandom(worldPos).nextInt(4))
                        .setValue(HoneycombBrood.FACING, blockState.getValue(HoneycombBrood.FACING)),
                    nbt);
        }

        return structureBlockInfoWorld;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return BzProcessors.HONEYCOMB_BROOD_RANDOMIZE_PROCESSOR.get();
    }
}