package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.WaypointFeatureConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(targets = "org.axial.axialutils.client.WarpWaypointManager", remap = false)
public abstract class WarpWaypointManagerMixin {
    @Inject(method = "getRenderTargets", at = @At("HEAD"), cancellable = true, remap = false)
    private static void axial_cosmetics$hideDisabledWaypoints(Object client, CallbackInfoReturnable<List<?>> cir) {
        if (!WaypointFeatureConfig.isEnabled()) cir.setReturnValue(List.of());
    }
}
