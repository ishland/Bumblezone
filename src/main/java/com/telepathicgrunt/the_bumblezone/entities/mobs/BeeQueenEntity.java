package com.telepathicgrunt.the_bumblezone.entities.mobs;

import com.telepathicgrunt.the_bumblezone.Bumblezone;
import com.telepathicgrunt.the_bumblezone.client.LivingEntityFlyingSoundInstance;
import com.telepathicgrunt.the_bumblezone.configs.BzBeeAggressionConfigs;
import com.telepathicgrunt.the_bumblezone.entities.queentrades.QueensTradeManager;
import com.telepathicgrunt.the_bumblezone.entities.queentrades.TradeEntryReducedObj;
import com.telepathicgrunt.the_bumblezone.modinit.BzEffects;
import com.telepathicgrunt.the_bumblezone.modinit.BzSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class BeeQueenEntity extends Animal {

    public final AnimationState idleAnimationState = new AnimationState();
    private static final EntityDataAccessor<Integer> THROWCOOLDOWN = SynchedEntityData.defineId(BeeQueenEntity.class, EntityDataSerializers.INT);

    public BeeQueenEntity(EntityType<? extends BeeQueenEntity> type, Level world) {
        super(type, world);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(THROWCOOLDOWN, 0);
    }

    public static AttributeSupplier.Builder getAttributeBuilder() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 150.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.1)
                .add(Attributes.ATTACK_DAMAGE, 10.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new AlwaysLookAtPlayerGoal(this, Player.class, 60));
        this.goalSelector.addGoal(3, new FloatGoal(this));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("throwcooldown", getThrowCooldown());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setThrowCooldown(tag.getInt("throwcooldown"));
    }

    @Override
    protected PathNavigation createNavigation(Level pLevel) {
        return new DirectPathNavigator(this, pLevel);
    }

    @Override
    protected MovementEmission getMovementEmission() {
        return MovementEmission.NONE;
    }

    @Override
    public MobType getMobType() {
        return MobType.ARTHROPOD;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source)) {
            return false;
        }
        else if(isOnPortalCooldown() && source == DamageSource.IN_WALL) {
            spawnMadParticles();
            playHurtSound(source);
            return false;
        }
        else {
            Entity entity = source.getEntity();

            if (BzBeeAggressionConfigs.aggressiveBees.get() && entity instanceof LivingEntity livingEntity) {

                if (!(livingEntity instanceof Player player && player.isCreative()) &&
                    (livingEntity.level.dimension().location().equals(Bumblezone.MOD_DIMENSION_ID) ||
                    BzBeeAggressionConfigs.allowWrathOfTheHiveOutsideBumblezone.get()) &&
                    !livingEntity.isSpectator() &&
                    BzBeeAggressionConfigs.aggressiveBees.get())
                {
                    if(livingEntity.hasEffect(BzEffects.PROTECTION_OF_THE_HIVE.get())) {
                        livingEntity.removeEffect(BzEffects.PROTECTION_OF_THE_HIVE.get());
                    }
                    else {
                        //Now all bees nearby in Bumblezone will get VERY angry!!!
                        livingEntity.addEffect(new MobEffectInstance(BzEffects.WRATH_OF_THE_HIVE.get(), BzBeeAggressionConfigs.howLongWrathOfTheHiveLasts.get(), 2, false, BzBeeAggressionConfigs.showWrathOfTheHiveParticles.get(), true));
                    }
                }
            }

            spawnMadParticles();
            return super.hurt(source, amount);
        }
    }

    public static boolean checkMobSpawnRules(EntityType<? extends Mob> entityType, LevelAccessor iWorld, MobSpawnType spawnReason, BlockPos blockPos, RandomSource random) {
        return true;
    }

    @Override
    public boolean checkSpawnRules(LevelAccessor world, MobSpawnType spawnReason) {
        return true;
    }

    @Override
    public boolean checkSpawnObstruction(LevelReader worldReader) {
        AABB box = getBoundingBox();
        return !worldReader.containsAnyLiquid(box) && worldReader.getBlockStates(box).noneMatch(state -> state.getMaterial().blocksMotion()) && worldReader.isUnobstructed(this);
    }

    private void spawnMadParticles() {
        if (!this.level.isClientSide()) {
            ((ServerLevel) this.level).sendParticles(
                    ParticleTypes.ANGRY_VILLAGER,
                    getX(),
                    getY(),
                    getZ(),
                    6,
                    this.level.getRandom().nextFloat() - 0.5f,
                    this.level.getRandom().nextFloat() * 0.4f + 0.4f,
                    this.level.getRandom().nextFloat() - 0.5f,
                    this.level.getRandom().nextFloat() * 0.8f + 0.4f);
        }
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockState) {
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return BzSounds.BEEHEMOTH_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return BzSounds.BEEHEMOTH_DEATH.get();
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket clientboundAddMobPacket) {
        super.recreateFromPacket(clientboundAddMobPacket);
        LivingEntityFlyingSoundInstance.playSound(this, BzSounds.BEEHEMOTH_LOOP.get());
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isAlive()) {
            this.idleAnimationState.startIfStopped(this.tickCount);
        }
        else {
            this.idleAnimationState.stop();
        }

        if (this.getAge() % 200 == 0) {
            this.heal(1);
        }

        if (!this.level.isClientSide()) {
            int throwCooldown = getThrowCooldown();
            if (throwCooldown > 0) {
                setThrowCooldown(throwCooldown - 1);
            }

            if (this.getAge() % 20 == 0 && throwCooldown <= 0) {
                Vec3 forwardVect = Vec3.directionFromRotation(0, this.getVisualRotationYInDegrees());
                Vec3 sideVect = Vec3.directionFromRotation(0, this.getVisualRotationYInDegrees() - 90);
                AABB scanArea = this.getBoundingBox().deflate(0.45).move(forwardVect.x() * 0.5d, -0.45, forwardVect.z() * 0.5d);
                List<ItemEntity> items = this.level.getEntitiesOfClass(ItemEntity.class, scanArea);
                items.stream().filter(ie -> !ie.hasPickUpDelay()).findFirst().ifPresent((itemEntity) -> {
                    boolean traded = false;
                    for (Map.Entry<Set<Item>, WeightedRandomList<TradeEntryReducedObj>> tradeEntries : QueensTradeManager.QUEENS_TRADE_MANAGER.tradeReduced.entrySet()) {
                        if (tradeEntries.getKey().contains(itemEntity.getItem().getItem())) {
                            for (int i = 0; i < itemEntity.getItem().getCount(); i++) {
                                Optional<TradeEntryReducedObj> reward = tradeEntries.getValue().getRandom(this.random);
                                if (reward.isPresent()) {
                                    spawnReward(forwardVect, sideVect, reward);
                                    traded = true;
                                }
                            }
                            if (traded) {
                                break;
                            }
                        }
                    }

                    if (traded) {
                        itemEntity.remove(RemovalReason.DISCARDED);
                    }
                    else {
                        itemEntity.remove(RemovalReason.DISCARDED);
                        ItemEntity rejectedItemEntity = new ItemEntity(
                                this.level,
                                this.getX() + (sideVect.x() * 1.75) + (forwardVect.x() * 1),
                                this.getY() + 0.3,
                                this.getZ() + (sideVect.z() * 1.75) + (forwardVect.x() * 1),
                                itemEntity.getItem(),
                                (this.random.nextFloat() - 0.5f) / 10 + forwardVect.x() / 3,
                                0.4f,
                                (this.random.nextFloat() - 0.5f) / 10 + forwardVect.z() / 3);
                        this.level.addFreshEntity(rejectedItemEntity);
                        rejectedItemEntity.setDefaultPickUpDelay();
                        spawnAngryParticles();
                    }

                    setThrowCooldown(50);
                });
            }
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Item item = stack.getItem();

        boolean traded = false;
        for (Map.Entry<Set<Item>, WeightedRandomList<TradeEntryReducedObj>> tradeEntries : QueensTradeManager.QUEENS_TRADE_MANAGER.tradeReduced.entrySet()) {
            if (tradeEntries.getKey().contains(item)) {
                if (this.level.isClientSide()) {
                    return InteractionResult.SUCCESS;
                }

                Vec3 forwardVect = Vec3.directionFromRotation(0, this.getVisualRotationYInDegrees());
                Vec3 sideVect = Vec3.directionFromRotation(0, this.getVisualRotationYInDegrees() - 90);

                Optional<TradeEntryReducedObj> reward = tradeEntries.getValue().getRandom(this.random);
                if (reward.isPresent()) {
                    spawnReward(forwardVect, sideVect, reward);
                    traded = true;
                }
                if (traded) {
                    break;
                }
            }
        }

        if (!this.level.isClientSide()) {
            if (!traded) {
                spawnAngryParticles();
            }
            else {
                setThrowCooldown(50);
                stack.shrink(1);
                player.setItemInHand(hand, stack);
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.PASS;
    }

    private void spawnReward(Vec3 forwardVect, Vec3 sideVect, Optional<TradeEntryReducedObj> reward) {
        ItemEntity rewardItemEntity = new ItemEntity(
                this.level,
                this.getX() + (sideVect.x() * 1.75) + (forwardVect.x() * 1),
                this.getY() + 0.3,
                this.getZ() + (sideVect.z() * 1.75) + (forwardVect.x() * 1),
                reward.get().item().getDefaultInstance(),
                (this.random.nextFloat() - 0.5f) / 10 + forwardVect.x() / 4,
                0.3f,
                (this.random.nextFloat() - 0.5f) / 10 + forwardVect.z() / 4);
        this.level.addFreshEntity(rewardItemEntity);
        rewardItemEntity.setDefaultPickUpDelay();
        spawnHappyParticles();
    }

    private void spawnAngryParticles() {
        ((ServerLevel)this.level).sendParticles(
                ParticleTypes.ANGRY_VILLAGER,
                getX(),
                getY() + 0.45f,
                getZ(),
                2,
                this.level.getRandom().nextFloat() - 0.5f,
                this.level.getRandom().nextFloat() * 0.4f + 0.4f,
                this.level.getRandom().nextFloat() - 0.5f,
                this.level.getRandom().nextFloat() * 0.8f + 0.4f);
    }

    private void spawnHappyParticles() {
        ((ServerLevel)this.level).sendParticles(
                ParticleTypes.HAPPY_VILLAGER,
                getX(),
                getY() + 0.75d,
                getZ(),
                5,
                0.8d,
                0.75d,
                0.8d,
                this.level.getRandom().nextFloat() + 0.5d);
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel serverWorld, AgeableMob ageableEntity) {
        return null;
    }

    @Override
    public int getHeadRotSpeed() {
        return 1;
    }

    @Override
    public int getMaxHeadXRot() {
        return 90;
    }

    public void onSyncedDataUpdated(EntityDataAccessor<?> entityDataAccessor) {
        if (DATA_POSE.equals(entityDataAccessor)) {
            Pose pose = this.getPose();
        }

        super.onSyncedDataUpdated(entityDataAccessor);
    }

    public int getThrowCooldown() {
        return this.entityData.get(THROWCOOLDOWN);
    }

    public void setThrowCooldown(Integer cooldown) {
        this.entityData.set(THROWCOOLDOWN, cooldown);
    }

    public static class DirectPathNavigator extends GroundPathNavigation {

        private final Mob mob;

        public DirectPathNavigator(Mob mob, Level world) {
            super(mob, world);
            this.mob = mob;
        }

        @Override
        public void tick() {
            ++this.tick;
        }

        @Override
        public boolean moveTo(double x, double y, double z, double speedIn) {
            mob.getMoveControl().setWantedPosition(x, y, z, speedIn);
            return true;
        }

        @Override
        public boolean moveTo(Entity entityIn, double speedIn) {
            mob.getMoveControl().setWantedPosition(entityIn.getX(), entityIn.getY(), entityIn.getZ(), speedIn);
            return true;
        }
    }


    public class AlwaysLookAtPlayerGoal extends Goal {
        protected final Mob mob;
        @Nullable
        protected Entity lookAt;
        protected final float lookDistance;
        private final boolean onlyHorizontal;
        protected final Class<? extends LivingEntity> lookAtType;
        protected final TargetingConditions lookAtContext;

        public AlwaysLookAtPlayerGoal(Mob mob, Class<? extends LivingEntity> lookAtType, float lookDistance) {
            this(mob, lookAtType, lookDistance, false);
        }

        public AlwaysLookAtPlayerGoal(Mob mob, Class<? extends LivingEntity> lookAtType, float lookDistance, boolean onlyHorizontal) {
            this.mob = mob;
            this.lookAtType = lookAtType;
            this.lookDistance = lookDistance;
            this.onlyHorizontal = onlyHorizontal;
            this.setFlags(EnumSet.of(Goal.Flag.LOOK));
            if (lookAtType == Player.class) {
                this.lookAtContext = TargetingConditions.forNonCombat().range(lookDistance).selector((livingEntity) -> EntitySelector.notRiding(mob).test(livingEntity));
            }
            else {
                this.lookAtContext = TargetingConditions.forNonCombat().range(lookDistance);
            }
        }

        public boolean canUse() {
            if (this.mob.getTarget() != null) {
                this.lookAt = this.mob.getTarget();
            }

            if (this.lookAtType == Player.class) {
                this.lookAt = this.mob.level.getNearestPlayer(this.lookAtContext, this.mob, this.mob.getX(), this.mob.getEyeY(), this.mob.getZ());
            }
            else {
                this.lookAt = this.mob.level.getNearestEntity(this.mob.level.getEntitiesOfClass(this.lookAtType, this.mob.getBoundingBox().inflate((double)this.lookDistance, 3.0D, (double)this.lookDistance), (p_148124_) -> true), this.lookAtContext, this.mob, this.mob.getX(), this.mob.getEyeY(), this.mob.getZ());
            }

            return this.lookAt != null;
        }

        public boolean canContinueToUse() {
            if (!this.lookAt.isAlive()) {
                return false;
            }
            else return !(this.mob.distanceToSqr(this.lookAt) > (double) (this.lookDistance * this.lookDistance));
        }

        public void start() {
        }

        public void stop() {
            this.lookAt = null;
        }

        public void tick() {
            if (this.lookAt != null && this.lookAt.isAlive()) {
                double y = this.onlyHorizontal ? this.mob.getEyeY() : this.lookAt.getEyeY();
                this.mob.getLookControl().setLookAt(this.lookAt.getX(), y, this.lookAt.getZ(), 0.05f, this.mob.getMaxHeadXRot());
            }
        }
    }
}
