package com.telepathicgrunt.the_bumblezone.worldgen.processors;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.telepathicgrunt.the_bumblezone.modinit.BzProcessors;
import com.telepathicgrunt.the_bumblezone.utils.GeneralUtils;
import com.telepathicgrunt.the_bumblezone.utils.OpenSimplex2F;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * Replace blocks randomly with noise generator but preserve the properties of the block
 */
public class NoiseReplaceWithPropertiesProcessor extends StructureProcessor {

    public static final MapCodec<NoiseReplaceWithPropertiesProcessor> CODEC = RecordCodecBuilder.mapCodec((instance) -> instance.group(
            BuiltInRegistries.BLOCK.byNameCodec().fieldOf("input_block").forGetter(config -> config.inputBlock),
            BuiltInRegistries.BLOCK.byNameCodec().fieldOf("output_block").forGetter(config -> config.outputBlock),
            Codec.floatRange(0, 1).fieldOf("threshold").forGetter(config -> config.threshold),
            Codec.FLOAT.fieldOf("xz_scale").forGetter(config -> config.xzScale),
            Codec.FLOAT.fieldOf("y_scale").forGetter(config -> config.yScale)
    ).apply(instance, instance.stable(NoiseReplaceWithPropertiesProcessor::new)));

    private final Block inputBlock;
    private final Block outputBlock;
    private final float threshold;
    private final float xzScale;
    private final float yScale;
    protected long seed;
    private OpenSimplex2F noiseGenerator = null;

    public NoiseReplaceWithPropertiesProcessor(Block inputBlock, Block outputBlock, float threshold, float xzScale, float yScale) {
        this.inputBlock = inputBlock;
        this.outputBlock = outputBlock;
        this.threshold = threshold;
        this.xzScale = xzScale;
        this.yScale = yScale;
    }

    public void setSeed(long seed) {
        if (this.seed != seed || noiseGenerator == null) {
            noiseGenerator = new OpenSimplex2F(seed);
            this.seed = seed;
        }
    }

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader levelReader, BlockPos pos, BlockPos pos2, StructureTemplate.StructureBlockInfo infoIn1, StructureTemplate.StructureBlockInfo structureBlockInfoWorld, StructurePlaceSettings settings) {
        if (GeneralUtils.isOutsideCenterWorldgenRegionChunk(levelReader, structureBlockInfoWorld.pos())) {
            return structureBlockInfoWorld;
        }

        setSeed(levelReader instanceof WorldGenRegion ? ((WorldGenRegion) levelReader).getSeed() : 0);

        if (structureBlockInfoWorld.state().getBlock() == inputBlock) {
            BlockPos worldPos = structureBlockInfoWorld.pos();
            double noiseVal = noiseGenerator.noise3_Classic(worldPos.getX() * xzScale, worldPos.getY() * yScale, worldPos.getZ() * xzScale);

            if ((noiseVal / 2D) + 0.5D < threshold) {
                BlockState newBlockState = outputBlock.defaultBlockState();
                for (Property<?> property : structureBlockInfoWorld.state().getProperties()) {
                    if (newBlockState.hasProperty(property)) {
                        newBlockState = GeneralUtils.getStateWithProperty(newBlockState, structureBlockInfoWorld.state(), property);
                    }
                }
                return new StructureTemplate.StructureBlockInfo(structureBlockInfoWorld.pos(), newBlockState, structureBlockInfoWorld.nbt());
            }
        }
        return structureBlockInfoWorld;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return BzProcessors.NOISE_REPLACE_WITH_PROPERTIES_PROCESSOR.get();
    }
}
