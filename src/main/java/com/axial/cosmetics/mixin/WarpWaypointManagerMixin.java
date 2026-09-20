package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.WaypointConfig;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(targets = "org.axial.axialutils.client.WarpWaypointManager", remap = false)
public abstract class WarpWaypointManagerMixin {
    @Inject(method = "getRenderTargets", at = @At("HEAD"), cancellable = true, remap = false)
    private static void axial_cosmetics$hideDisabledWaypoints(MinecraftClient client, CallbackInfoReturnable<List<?>> cir) {
        if (!WaypointConfig.enabled()) cir.setReturnValue(List.of());
    }
}
