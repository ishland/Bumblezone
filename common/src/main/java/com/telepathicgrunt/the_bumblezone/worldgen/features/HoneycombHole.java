package com.telepathicgrunt.the_bumblezone.worldgen.features;

import com.mojang.serialization.Codec;
import com.telepathicgrunt.the_bumblezone.Bumblezone;
import com.telepathicgrunt.the_bumblezone.mixin.world.StructureTemplateAccessor;
import com.telepathicgrunt.the_bumblezone.utils.GeneralUtils;
import com.telepathicgrunt.the_bumblezone.worldgen.features.configs.NbtFeatureConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.List;
import java.util.Optional;


public class HoneycombHole extends Feature<NbtFeatureConfig> {

    private static final ResourceLocation EMPTY = ResourceLocation.fromNamespaceAndPath("minecraft", "empty");

    public HoneycombHole(Codec<NbtFeatureConfig> configFactory) {
        super(configFactory);
    }

    @Override
    public boolean place(FeaturePlaceContext<NbtFeatureConfig> context) {
        ResourceLocation nbtRL = GeneralUtils.getRandomEntry(context.config().nbtResourcelocationsAndWeights, context.random());

        StructureTemplateManager structureManager = context.level().getLevel().getStructureManager();
        StructureTemplate template = structureManager.get(nbtRL).orElseThrow(() -> {
            String errorMsg = "Honeycomb cave NBT not found!";
            Bumblezone.LOGGER.error(errorMsg);
            return new RuntimeException(errorMsg);
        });

        // For proper offsetting the feature.
        BlockPos halfLengths = new BlockPos(
                template.getSize().getX() / 2,
                template.getSize().getY() / 2,
                template.getSize().getZ() / 2);

        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos().set(context.origin());

        // offset the feature's position
        BlockPos position = context.origin().above(context.config().structureYOffset);

        StructurePlaceSettings structurePlacementData = (new StructurePlaceSettings()).setRotation(Rotation.NONE).setLiquidSettings(LiquidSettings.IGNORE_WATERLOGGING).setRotationPivot(halfLengths).setIgnoreEntities(false).setKnownShape(true);
        Registry<StructureProcessorList> processorListRegistry = context.level().getLevel().getServer().registryAccess().registryOrThrow(Registries.PROCESSOR_LIST);
        StructureProcessorList emptyProcessor = processorListRegistry.get(EMPTY);

        Optional<StructureProcessorList> processor = processorListRegistry.getOptional(context.config().processor);
        processor.orElse(emptyProcessor).list().forEach(structurePlacementData::addProcessor); // add all processors
        mutable.set(position).move(-halfLengths.getX(), 0, -halfLengths.getZ());
        GeneralUtils.placeInWorldWithChunkSectionCachingAndWithoutNeighborUpdate(context.level(), template, mutable, mutable, structurePlacementData, context.random(), Block.UPDATE_INVISIBLE);

        // Post-processors
        // For all processors that are sensitive to neighboring blocks such as vines.
        // Post processors will place the blocks themselves so we will not do anything with the return of Structure.process
        structurePlacementData.clearProcessors();
        Optional<StructureProcessorList> postProcessor = processorListRegistry.getOptional(context.config().postProcessor);
        postProcessor.orElse(emptyProcessor).list().forEach(structurePlacementData::addProcessor); // add all post processors
        List<StructureTemplate.StructureBlockInfo> list = structurePlacementData.getRandomPalette(((StructureTemplateAccessor)template).getBlocks(), mutable).blocks();
        StructureTemplate.processBlockInfos(context.level(), mutable, mutable, structurePlacementData, list);

        return true;
    }
}