package com.axial.cosmetics.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps the CPS HUD readable by displaying whole clicks rather than decimal padding. */
@Mixin(targets = "org.axial.axialutils.client.CpsTracker", remap = false)
public abstract class CpsTrackerMixin {
    @Inject(method = "getClicksPerSecondText", at = @At("RETURN"), cancellable = true)
    private static void axial_cosmetics$useWholeClickCount(MinecraftClient client, CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(cir.getReturnValue().replaceFirst("(?<=\\d)\\.\\d+", ""));
    }
}
