package com.telepathicgrunt.the_bumblezone.mixin.world;

import com.telepathicgrunt.the_bumblezone.Bumblezone;
import com.telepathicgrunt.the_bumblezone.entities.BeeDedicatedSpawning;
import com.telepathicgrunt.the_bumblezone.utils.GeneralUtils;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public class ServerLevelMixin {

	@Unique
	private static final int thebumblezone_updateInterval = 20;

	@Unique
	private static int thebumblezone_counter = 0;

	@Inject(
			method = "tick(Ljava/util/function/BooleanSupplier;)V",
			at = @At(value = "HEAD")
	)
	private void thebumblezone_countBzDimEntities(CallbackInfo ci) {
		ServerLevel world = ((ServerLevel) (Object) this);
		if(world.dimension().location().equals(Bumblezone.MOD_DIMENSION_ID)) {
			thebumblezone_counter++;
			if(thebumblezone_counter % thebumblezone_updateInterval == 0) {
				thebumblezone_counter = 0;
				GeneralUtils.updateEntityCount(world);
				if(Bumblezone.BZ_CONFIG.BZGeneralConfig.specialBeeSpawning) {
					BeeDedicatedSpawning.specialSpawnBees((ServerLevel) (Object) this);
				}
			}
		}
	}
}
