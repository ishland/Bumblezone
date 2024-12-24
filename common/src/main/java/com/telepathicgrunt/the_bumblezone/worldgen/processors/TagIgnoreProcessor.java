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

public class TagIgnoreProcessor extends StructureProcessor {

    public static final MapCodec<TagIgnoreProcessor> CODEC = RecordCodecBuilder.mapCodec((instance) -> instance.group(
            TagKey.codec(Registries.BLOCK).fieldOf("ignore_blocks_tag").forGetter(config -> config.ignoreBlocksTag)
    ).apply(instance, instance.stable(TagIgnoreProcessor::new)));

    private final TagKey<Block> ignoreBlocksTag;

    public TagIgnoreProcessor(TagKey<Block> ignoreBlocksTag) {
        this.ignoreBlocksTag = ignoreBlocksTag;
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
        return worldState.is(this.ignoreBlocksTag) ? null : structureBlockInfoWorld;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return BzProcessors.TAG_IGNORE_PROCESSOR.get();
    }
}
