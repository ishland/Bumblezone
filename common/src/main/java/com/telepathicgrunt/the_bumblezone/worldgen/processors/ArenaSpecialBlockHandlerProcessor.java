package com.telepathicgrunt.the_bumblezone.worldgen.processors;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.telepathicgrunt.the_bumblezone.modinit.BzProcessors;
import com.telepathicgrunt.the_bumblezone.modinit.BzTags;
import com.telepathicgrunt.the_bumblezone.utils.EnchantmentUtils;
import com.telepathicgrunt.the_bumblezone.utils.GeneralUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class ArenaSpecialBlockHandlerProcessor extends StructureProcessor {

    public static final MapCodec<ArenaSpecialBlockHandlerProcessor> CODEC = RecordCodecBuilder.mapCodec((instance) -> instance.group(
            Codec.BOOL.fieldOf("clear_containers_only").forGetter(config -> config.clearContainersOnly)
    ).apply(instance, instance.stable(ArenaSpecialBlockHandlerProcessor::new)));

    private final boolean clearContainersOnly;

    public ArenaSpecialBlockHandlerProcessor(boolean clearContainersOnly) {
        this.clearContainersOnly = clearContainersOnly;
    }

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader levelReader, BlockPos pos, BlockPos blockPos, StructureTemplate.StructureBlockInfo structureBlockInfoLocal, StructureTemplate.StructureBlockInfo structureBlockInfoWorld, StructurePlaceSettings settings) {
        if (GeneralUtils.isOutsideStructureAllowedBounds(settings, structureBlockInfoWorld.pos())) {
            return structureBlockInfoWorld;
        }

        if (levelReader instanceof Level level) {
            BlockState inWorldBlockState = level.getBlockState(structureBlockInfoWorld.pos());

            if (inWorldBlockState.is(BzTags.ESSENCE_ARENA_DOES_NOT_REPLACE) && !this.clearContainersOnly) {
                BlockState structureBlockState = structureBlockInfoWorld.state();
                BlockEntity blockEntity = null;
                if (structureBlockState.getBlock() instanceof EntityBlock entityBlock) {
                    blockEntity = entityBlock.newBlockEntity(structureBlockInfoWorld.pos(), structureBlockState);
                    if (blockEntity != null) {
                        blockEntity.loadWithComponents(structureBlockInfoWorld.nbt(), levelReader.registryAccess());
                    }
                }
                ItemStack itemStack = new ItemStack(Items.DIAMOND_PICKAXE);
                itemStack.enchant(EnchantmentUtils.getEnchantmentHolder(Enchantments.SILK_TOUCH, level), 1);
                Block.dropResources(structureBlockState, level, structureBlockInfoWorld.pos(), blockEntity, null, itemStack);
                return null;
            }

            if (inWorldBlockState.hasBlockEntity()) {
                BlockEntity blockEntity = level.getBlockEntity(structureBlockInfoWorld.pos());
                if (blockEntity instanceof Container container) {
                    if (this.clearContainersOnly) {
                        container.clearContent();
                        container.setChanged();
                        level.setBlockEntity(blockEntity);
                    }
                    else {
                        ItemStack itemStack = new ItemStack(Items.DIAMOND_PICKAXE);
                        itemStack.enchant(EnchantmentUtils.getEnchantmentHolder(Enchantments.SILK_TOUCH, level), 1);
                        Block.dropResources(inWorldBlockState, level, structureBlockInfoWorld.pos(), blockEntity, null, itemStack);
                        level.destroyBlock(structureBlockInfoWorld.pos(), false);
                    }
                }
            }
        }

        return structureBlockInfoWorld;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return BzProcessors.ARENA_SPECIAL_BLOCK_HANDLER_PROCESSOR.get();
    }
}