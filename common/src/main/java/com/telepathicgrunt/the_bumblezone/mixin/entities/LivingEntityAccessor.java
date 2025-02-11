package com.telepathicgrunt.the_bumblezone.mixin.entities;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Accessor("jumping")
    boolean isJumping();

    @Invoker("isAlwaysExperienceDropper")
    boolean callIsAlwaysExperienceDropper();

    @Accessor("lastDamageSource")
    void setLastDamageSource(DamageSource lastDamageSource);

    @Accessor("lastDamageStamp")
    void setLastDamageStamp(long lastDamageStamp);

    @Invoker("hurtArmor")
    void callHurtArmor(DamageSource damageSource, float f);
}
