package com.telepathicgrunt.the_bumblezone.blocks;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.telepathicgrunt.the_bumblezone.blocks.blockentities.StateFocusedBrushableBlockEntity;
import com.telepathicgrunt.the_bumblezone.items.HoneyBeeLeggings;
import com.telepathicgrunt.the_bumblezone.mixin.entities.EntityCollisionContextAccessor;
import com.telepathicgrunt.the_bumblezone.modinit.BzBlocks;
import com.telepathicgrunt.the_bumblezone.modinit.BzCriterias;
import com.telepathicgrunt.the_bumblezone.modinit.BzEntities;
import com.telepathicgrunt.the_bumblezone.modinit.BzItems;
import com.telepathicgrunt.the_bumblezone.modinit.BzParticles;
import com.telepathicgrunt.the_bumblezone.modinit.BzTags;
import com.telepathicgrunt.the_bumblezone.utils.GeneralUtils;
import com.telepathicgrunt.the_bumblezone.utils.PlatformHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Panda;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BrushItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BrushableBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

import static com.telepathicgrunt.the_bumblezone.blocks.PileOfPollen.APPLIED_FALL_REDUCTION_FOR_ENTITY;
import static com.telepathicgrunt.the_bumblezone.blocks.PileOfPollen.slowFallSpeed;

public class PileOfPollenSuspicious extends BrushableBlock implements StateReturningBrushableBlock {
    protected static final VoxelShape SHAPE = Shapes.block();
    private Item item;

    public static final MapCodec<? extends PileOfPollenSuspicious> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BuiltInRegistries.BLOCK.byNameCodec().fieldOf("turns_into").forGetter(PileOfPollenSuspicious::getTurnsInto),
            BuiltInRegistries.SOUND_EVENT.byNameCodec().fieldOf("brush_sound").forGetter(PileOfPollenSuspicious::getBrushSound),
            BuiltInRegistries.SOUND_EVENT.byNameCodec().fieldOf("brush_comleted_sound").forGetter(PileOfPollenSuspicious::getBrushCompletedSound), PileOfPollenSuspicious.propertiesCodec()
    ).apply(instance, PileOfPollenSuspicious::new));

    public PileOfPollenSuspicious() {
        this(null,
                SoundEvents.BRUSH_SAND,
                SoundEvents.BRUSH_SAND_COMPLETED,
                Properties.of()
                .mapColor(MapColor.COLOR_YELLOW)
                .isViewBlocking((blockState, world, blockPos) -> true)
                .isSuffocating((blockState, blockGetter, blockPos) -> false)
                .noOcclusion()
                .noCollission()
                .strength(0.1F)
                .pushReaction(PushReaction.DESTROY)
                .sound(SoundType.SNOW));
    }

    public PileOfPollenSuspicious(Block block, SoundEvent soundEvent1, SoundEvent soundEvent2, BlockBehaviour.Properties properties) {
        super(block, soundEvent1, soundEvent2, properties);
    }

    @Override
    public MapCodec<BrushableBlock> codec() {
        return (MapCodec<BrushableBlock>)(MapCodec<?>)CODEC;
    }

    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new StateFocusedBrushableBlockEntity(blockPos, blockState);
    }

    public Block getTurnsInto() {
        return BzBlocks.PILE_OF_POLLEN.get();
    }

    @Override
    public BlockState getTurnsIntoState() {
        return BzBlocks.PILE_OF_POLLEN.get().defaultBlockState().setValue(PileOfPollen.LAYERS, 8);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader world, BlockPos blockPos, BlockState blockState) {
        return new ItemStack(BzItems.POLLEN_PUFF.get());
    }

    /**
     * Makes this block spawn Pollen Puff when broken by piston or falling block breaks
     */
    @Override
    public Item asItem() {
        if (this.item == null) {
            this.item = BzItems.POLLEN_PUFF.get();
        }

        return this.item;
    }

    @Override
    protected boolean isPathfindable(BlockState blockState, PathComputationType pathComputationType) {
        return true;
    }

    @Override
    public VoxelShape getShape(BlockState blockState, BlockGetter world, BlockPos blockPos, CollisionContext selectionContext) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        if (context instanceof EntityCollisionContext ctx) {
            Entity entity = ctx.getEntity();
            if (entity != null && entity.getType() != BzEntities.POLLEN_PUFF_ENTITY.get()) {
                context.isHoldingItem(Items.AIR);
                ItemStack heldItem = ((EntityCollisionContextAccessor)ctx).getHeldItem();
                if (heldItem != null &&
                    !heldItem.isEmpty() &&
                    (PlatformHooks.isItemAbility(heldItem, BrushItem.class, "brush_brush") ||
                    (heldItem.is(BzTags.SUSPICIOUS_PILE_OF_POLLEN_ADDITIONAL_BRUSHES))))
                {
                    StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
                    boolean found = Arrays.stream(stackTrace)
                            .map(StackTraceElement::getClassName)
                            .anyMatch((c) -> c.equals(Items.BRUSH.getClass().getName()) || c.equals(heldItem.getItem().getClass().getName()));

                    if (found) {
                        return getShape(state, worldIn, pos, context);
                    }
                }

                return Shapes.empty();
            }
        }
        return getShape(state, worldIn, pos, context);
    }


    @Override
    public VoxelShape getBlockSupportShape(BlockState blockState, BlockGetter world, BlockPos blockPos) {
        return SHAPE;
    }

    @Override
    public VoxelShape getVisualShape(BlockState blockState, BlockGetter world, BlockPos blockPos, CollisionContext selectionContext) {
        return SHAPE;
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState blockState) {
        return true;
    }

    @Override
    public boolean canSurvive(BlockState blockState, LevelReader world, BlockPos blockPos) {
        BlockState blockstate = world.getBlockState(blockPos.below());
        if(blockstate.is(BlockTags.SNOW_LAYER_CANNOT_SURVIVE_ON) || !world.getBlockState(blockPos).getFluidState().isEmpty()) {
            return false;
        }
        else if(blockstate.isAir() || blockstate.is(BzTags.POLLEN_BLOCKS) || blockstate.is(BlockTags.SNOW_LAYER_CAN_SURVIVE_ON)) {
            return true;
        }
        else {
            return GeneralUtils.isFaceFullFast(blockstate.getCollisionShape(world, blockPos.below()), Direction.UP);
        }
    }

    @Override
    public void tick(BlockState blockState, ServerLevel serverLevel, BlockPos blockPos, RandomSource randomSource) {
        BlockEntity blockEntity = serverLevel.getBlockEntity(blockPos);
        if (blockEntity instanceof BrushableBlockEntity brushableBlockEntity) {
            brushableBlockEntity.checkReset();
        }
        BlockState belowState = serverLevel.getBlockState(blockPos.below());
        if (!FallingBlock.isFree(belowState) || (belowState.is(BzBlocks.PILE_OF_POLLEN.get()) && belowState.getValue(PileOfPollen.LAYERS) == 8) || blockPos.getY() < serverLevel.getMinBuildHeight()) {
            return;
        }
        FallingBlockEntity fallingBlockEntity = FallingBlockEntity.fall(serverLevel, blockPos, blockState);
        fallingBlockEntity.disableDrop();
    }

    @Override
    public BlockState updateShape(BlockState oldBlockState, Direction direction, BlockState newBlockState, LevelAccessor world, BlockPos blockPos, BlockPos blockPos1) {
        return !oldBlockState.canSurvive(world, blockPos) ? Blocks.AIR.defaultBlockState() : super.updateShape(oldBlockState, direction, newBlockState, world, blockPos, blockPos1);
    }

    @Override
    public void destroy(LevelAccessor world, BlockPos blockPos, BlockState blockState) {
        if(world.isClientSide()) {
            for(int i = 0; i < 50; i++) {
                spawnParticles(blockState, world, blockPos, world.getRandom(), true);
                spawnParticles(world, Vec3.atCenterOf(blockPos), world.getRandom(), 0.055D, 0.0075D, 0);
            }
        }
    }

    /**
     * Slows all entities inside the block.
     */
    @Override
    public void entityInside(BlockState blockState, Level world, BlockPos blockPos, Entity entity) {
        if (!blockState.is(BzBlocks.PILE_OF_POLLEN_SUSPICIOUS.get())) {
            return;
        }

        // slows the entity and spawns particles
        if (!(entity instanceof ExperienceOrb)) {
            int layerValueMinusOne = 7;
            double entitySpeed = entity.getDeltaMovement().length();
            double chance = 0.22D + layerValueMinusOne * 0.09D;

            Pair<Integer, Integer> reduction = APPLIED_FALL_REDUCTION_FOR_ENTITY.getOrDefault(entity.getStringUUID(), null);
            if (reduction == null ||
                    reduction.getFirst() != entity.tickCount ||
                    (reduction.getSecond() < layerValueMinusOne && reduction.getFirst() == entity.tickCount))
            {
                int diffInLayer = reduction == null ? layerValueMinusOne : layerValueMinusOne - reduction.getSecond();
                slowFallSpeed(entity, blockPos, diffInLayer);
                APPLIED_FALL_REDUCTION_FOR_ENTITY.put(entity.getStringUUID(), Pair.of(entity.tickCount, layerValueMinusOne));
            }

            // Need to multiply speed to avoid issues where tiny movement is seen as zero.
            if(entitySpeed > 0.00001D && world.random.nextFloat() < chance) {
                int particleNumber = (int) (entitySpeed / 0.0045D);
                int particleStrength = (entity instanceof ItemEntity) ? Math.min(10, particleNumber / 3) : Math.min(20, particleNumber);

                if(world.isClientSide()) {
                    for(int i = 0; i < particleNumber; i++) {
                        if(particleNumber > 5) spawnParticles(blockState, world, blockPos, world.random, true);

                        spawnParticles(
                                world,
                                entity.position()
                                        .add(entity.getDeltaMovement().multiply(2D, 2D, 2D))
                                        .add(0, 0.75D, 0),
                                world.random,
                                0.006D * particleStrength,
                                0.00075D * particleStrength,
                                0.006D * particleStrength);
                    }
                }
                // Player and item entity runs this method on client side already so do not run it on server to reduce particle packet spam
                else if (!(entity instanceof Player || entity instanceof ItemEntity)) {
                    spawnParticlesServer(
                            world,
                            entity.position()
                                    .add(entity.getDeltaMovement().multiply(2D, 2D, 2D))
                                    .add(0, 0.75D, 0),
                            world.random,
                            0.006D * particleStrength,
                            0.00075D * particleStrength,
                            0.006D * particleStrength,
                            particleNumber);
                }
            }

            // make pandas sneeze
            if(entity instanceof Panda pandaEntity) {
                pandaSneezing(pandaEntity);
            }

            // make entity invisible if hidden inside
            if(entity instanceof LivingEntity livingEntity) {
                PileOfPollen.applyHiddenEffectIfBuried(livingEntity, blockState, blockPos, true);
            }
        }
    }

    public static void pandaSneezing(Panda pandaEntity) {
        if(!pandaEntity.level().isClientSide()) {
            if(pandaEntity.getRandom().nextFloat() < 0.005f && pandaEntity.level().getBlockState(pandaEntity.blockPosition()).is(BzBlocks.PILE_OF_POLLEN.get())) {
                pandaEntity.sneeze(true);
            }
        }
    }

    // Rarely spawn particle on its own
    public void animateTick(BlockState blockState, Level world, BlockPos blockPos, RandomSource random) {
        double chance = 0.080f;
        if(random.nextFloat() < chance) spawnParticles(blockState, world, blockPos, random, false);
    }

    public static void spawnParticles(BlockState blockState, LevelAccessor world, BlockPos blockPos, RandomSource random, boolean disturbed) {
        for(Direction direction : Direction.values()) {
            BlockPos blockpos = blockPos.relative(direction);
            if (!world.getBlockState(blockpos).isSolidRender(world, blockpos)) {
                double speedYModifier = disturbed ? 0.05D : 0.005D;
                double speedXZModifier = disturbed ? 0.03D : 0.005D;
                VoxelShape currentShape = SHAPE;
                double yHeight = currentShape.max(Direction.Axis.Y) - currentShape.min(Direction.Axis.Y);

                Direction.Axis directionAxis = direction.getAxis();
                double xOffset = directionAxis == Direction.Axis.X ? 0.5D + 0.5625D * (double)direction.getStepX() : (double)random.nextFloat();
                double yOffset = directionAxis == Direction.Axis.Y ? yHeight * (double)direction.getStepY() : (double)random.nextFloat() * yHeight;
                double zOffset = directionAxis == Direction.Axis.Z ? 0.5D + 0.5625D * (double)direction.getStepZ() : (double)random.nextFloat();

                world.addParticle(
                        BzParticles.POLLEN_PARTICLE.get(),
                        (double)blockPos.getX() + xOffset,
                        (double)blockPos.getY() + yOffset,
                        (double)blockPos.getZ() + zOffset,
                        random.nextGaussian() * speedXZModifier,
                        (random.nextGaussian() * speedYModifier) + (disturbed ? 0.01D : 0),
                        random.nextGaussian() * speedXZModifier);

                return;
            }
        }
    }

    public static void spawnParticles(LevelAccessor world, Vec3 location, RandomSource random, double speedXZModifier, double speedYModifier, double initYSpeed) {
        double xOffset = (random.nextFloat() * 0.3) - 0.15;
        double yOffset = (random.nextFloat() * 0.3) - 0.15;
        double zOffset = (random.nextFloat() * 0.3) - 0.15;

        world.addParticle(
                BzParticles.POLLEN_PARTICLE.get(),
                location.x() + xOffset,
                location.y() + yOffset,
                location.z() + zOffset,
                random.nextGaussian() * speedXZModifier,
                (random.nextGaussian() * speedYModifier) + initYSpeed,
                random.nextGaussian() * speedXZModifier);
    }

    public static void spawnParticlesServer(LevelAccessor world, Vec3 location, RandomSource random, double speedXZModifier, double speedYModifier, double initYSpeed, int numberOfParticles) {
        if(world.isClientSide()) return;

        double xOffset = (random.nextFloat() * 0.3) - 0.15;
        double yOffset = (random.nextFloat() * 0.3) - 0.15;
        double zOffset = (random.nextFloat() * 0.3) - 0.15;

        ((ServerLevel)world).sendParticles(
                BzParticles.POLLEN_PARTICLE.get(),
                location.x() + xOffset,
                location.y() + yOffset,
                location.z() + zOffset,
                numberOfParticles,
                random.nextGaussian() * speedXZModifier,
                (random.nextGaussian() * speedYModifier) + initYSpeed,
                random.nextGaussian() * speedXZModifier,
                0.02f
        );
    }
}
