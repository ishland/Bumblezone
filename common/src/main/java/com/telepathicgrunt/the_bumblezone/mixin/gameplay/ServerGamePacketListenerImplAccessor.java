package com.telepathicgrunt.the_bumblezone.mixin.gameplay;

import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerGamePacketListenerImpl.class)
public interface ServerGamePacketListenerImplAccessor {
    @Accessor
    void setAboveGroundTickCount(int aboveGroundTickCount);

    @Accessor
    void setAboveGroundVehicleTickCount(int aboveGroundVehicleTickCount);
}
