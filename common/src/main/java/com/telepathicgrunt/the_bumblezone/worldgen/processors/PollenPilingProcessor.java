package com.telepathicgrunt.the_bumblezone.worldgen.processors;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.telepathicgrunt.the_bumblezone.blocks.PileOfPollen;
import com.telepathicgrunt.the_bumblezone.modinit.BzBlocks;
import com.telepathicgrunt.the_bumblezone.modinit.BzProcessors;
import com.telepathicgrunt.the_bumblezone.utils.GeneralUtils;
import com.telepathicgrunt.the_bumblezone.utils.OpenSimplex2F;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class PollenPilingProcessor extends StructureProcessor {

    public static final MapCodec<PollenPilingProcessor> CODEC = RecordCodecBuilder.mapCodec((instance) -> instance.group(
            Codec.FLOAT.fieldOf("xz_scale").forGetter(config -> config.xzScale),
            Codec.FLOAT.fieldOf("y_scale").forGetter(config -> config.yScale),
            Codec.BOOL.fieldOf("pollen_replace_solids").forGetter(config -> config.pollenReplaceSolids)
    ).apply(instance, instance.stable(PollenPilingProcessor::new)));

    private final float xzScale;
    private final float yScale;
    private final boolean pollenReplaceSolids;
    protected long seed;
    private OpenSimplex2F noiseGenerator = null;

    public PollenPilingProcessor(float xzScale, float yScale, boolean pollenReplaceSolids) {
        this.xzScale = xzScale;
        this.yScale = yScale;
        this.pollenReplaceSolids = pollenReplaceSolids;
    }

    public void setSeed(long seed) {
        if (this.seed != seed || noiseGenerator == null) {
            noiseGenerator = new OpenSimplex2F(seed);
            this.seed = seed;
        }
    }

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader levelReader, BlockPos pos, BlockPos blockPos, StructureTemplate.StructureBlockInfo structureBlockInfoLocal, StructureTemplate.StructureBlockInfo structureBlockInfoWorld, StructurePlaceSettings settings) {
        if (GeneralUtils.isOutsideStructureAllowedBounds(settings, structureBlockInfoWorld.pos())) {
            return structureBlockInfoWorld;
        }

        setSeed(levelReader instanceof WorldGenRegion ? ((WorldGenRegion) levelReader).getSeed() : 0);
        StructureTemplate.StructureBlockInfo structureBlockInfoToReturn = structureBlockInfoWorld;
        BlockState structureState = structureBlockInfoToReturn.state();
        BlockPos worldPos = structureBlockInfoToReturn.pos();

        if (structureState.is(BzBlocks.PILE_OF_POLLEN.get())) {
            if (!pollenReplaceSolids && !levelReader.getBlockState(worldPos).isAir()) {
                return null;
            }

            BlockPos belowPos = worldPos.below();
            if (belowPos.getY() <= levelReader.getMinBuildHeight() || belowPos.getY() >= levelReader.getMaxBuildHeight()) {
                return null;
            }
            
            ChunkAccess chunk = levelReader.getChunk(belowPos);
            BlockState belowState = chunk.getBlockState(belowPos);
            if (!belowState.canOcclude()) {
                ((LevelAccessor)levelReader).scheduleTick(belowPos, structureState.getBlock(), 0);
            }

            BlockPos.MutableBlockPos sidePos = new BlockPos.MutableBlockPos();
            for (Direction direction : Direction.values()) {
                sidePos.set(worldPos).move(direction);
                if (levelReader.getBlockState(sidePos).getFluidState().isSource()) {
                    return new StructureTemplate.StructureBlockInfo(worldPos, BzBlocks.FILLED_POROUS_HONEYCOMB.get().defaultBlockState(), null);
                }
            }

            double noiseVal = noiseGenerator.noise3_Classic(worldPos.getX() * xzScale, worldPos.getY() * yScale, worldPos.getZ() * xzScale);
            int layerHeight = Math.max(0, (int) (((noiseVal / 2D) + 0.5D) * 2.5D));
            layerHeight = Math.min(8, layerHeight + structureState.getValue(PileOfPollen.LAYERS));
            structureBlockInfoToReturn = new StructureTemplate.StructureBlockInfo(worldPos, structureState.setValue(PileOfPollen.LAYERS, layerHeight), structureBlockInfoToReturn.nbt());
        }

        if (!structureState.canOcclude()) {
            BlockPos abovePos = worldPos.above();
            ChunkAccess chunk = levelReader.getChunk(abovePos);
            BlockState aboveState = chunk.getBlockState(abovePos);
            if (aboveState.is(BzBlocks.PILE_OF_POLLEN.get())) {
                ((LevelAccessor)levelReader).scheduleTick(abovePos, aboveState.getBlock(), 0);
            }
        }

        return structureBlockInfoToReturn;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return BzProcessors.POLLEN_PILING_PROCESSOR.get();
    }
}