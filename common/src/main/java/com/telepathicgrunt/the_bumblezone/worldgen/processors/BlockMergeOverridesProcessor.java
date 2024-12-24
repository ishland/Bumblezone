package com.telepathicgrunt.the_bumblezone.worldgen.processors;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.telepathicgrunt.the_bumblezone.modinit.BzProcessors;
import com.telepathicgrunt.the_bumblezone.utils.GeneralUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class BlockMergeOverridesProcessor extends StructureProcessor {

    public static final MapCodec<BlockMergeOverridesProcessor> CODEC = RecordCodecBuilder.mapCodec((instance) -> instance.group(
            TagKey.codec(Registries.BLOCK).fieldOf("overridable_blocks").forGetter(config -> config.overridableBlocks),
            TagKey.codec(Registries.BLOCK).fieldOf("overruling_blocks").forGetter(config -> config.overrulingBlocks)
    ).apply(instance, instance.stable(BlockMergeOverridesProcessor::new)));

    private final TagKey<Block> overridableBlocks;
    private final TagKey<Block> overrulingBlocks;

    public BlockMergeOverridesProcessor(TagKey<Block> overridableBlocks, TagKey<Block> overrulingBlocks) {
        this.overridableBlocks = overridableBlocks;
        this.overrulingBlocks = overrulingBlocks;
    }

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader levelReader, BlockPos pos, BlockPos pos2, StructureTemplate.StructureBlockInfo infoIn1, StructureTemplate.StructureBlockInfo structureBlockInfoWorld, StructurePlaceSettings settings) {
        if (structureBlockInfoWorld == null) {
            return null;
        }

        if (GeneralUtils.isOutsideStructureAllowedBounds(settings, structureBlockInfoWorld.pos())) {
            return structureBlockInfoWorld;
        }

        BlockState worldState = levelReader.getBlockState(structureBlockInfoWorld.pos());
        BlockState currentState = structureBlockInfoWorld.state();

        if (worldState.is(this.overrulingBlocks) && currentState.is(this.overridableBlocks)) {
            return null;
        }

        return structureBlockInfoWorld;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return BzProcessors.BLOCK_MERGE_OVERRIDES_PROCESSOR.get();
    }
}
