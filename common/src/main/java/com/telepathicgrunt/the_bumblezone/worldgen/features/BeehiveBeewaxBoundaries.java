package com.telepathicgrunt.the_bumblezone.worldgen.features;

import com.mojang.serialization.Codec;
import com.telepathicgrunt.the_bumblezone.modinit.BzBlocks;
import com.telepathicgrunt.the_bumblezone.utils.UnsafeBulkSectionAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;


public class BeehiveBeewaxBoundaries extends Feature<NoneFeatureConfiguration> {

    public BeehiveBeewaxBoundaries(Codec<NoneFeatureConfiguration> code) {
        super(code);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {

        BlockState state = BzBlocks.BEEHIVE_BEESWAX.get().defaultBlockState();
        RandomSource randomSource = context.random();
        UnsafeBulkSectionAccess bulkSectionAccess = new UnsafeBulkSectionAccess(context.level());
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        int minY = context.chunkGenerator().getMinY();
        int maxY = minY + context.chunkGenerator().getGenDepth();

        // Ceiling
        for (int y = maxY - 1; y >= maxY - 6; y--) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    mutableBlockPos.set(context.origin()).move(x, y, z);
                    int diff = maxY - y;
                    if (diff <= 4 || ((diff - 4) / 3f < randomSource.nextFloat())) {
                        bulkSectionAccess.setBlockState(mutableBlockPos, state, false);
                    }
                }
            }
        }

        // Floor
        for (int y = minY; y <= minY + 6; y++) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    mutableBlockPos.set(context.origin()).move(x, y, z);
                    int diff = y - minY;
                    if (diff <= 4 || ((diff - 4) / 3f < randomSource.nextFloat())) {
                        bulkSectionAccess.setBlockState(mutableBlockPos, state, false);
                    }
                }
            }
        }

        return true;
    }
}