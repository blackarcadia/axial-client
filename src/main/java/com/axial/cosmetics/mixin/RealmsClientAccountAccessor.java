package com.axial.cosmetics.mixin;

import net.minecraft.client.realms.RealmsClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RealmsClient.class)
public interface RealmsClientAccountAccessor {
    @Accessor("instance")
    static void axial$setInstance(RealmsClient instance) {
        throw new AssertionError("Mixin accessor");
    }
}
