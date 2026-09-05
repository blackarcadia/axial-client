package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.FissureHighlightRemoval;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "org.axial.axialutils.client.AxialConfigManager", remap = false)
public abstract class AxialConfigManagerFissureRemovalMixin {
    @Inject(method = "load", at = @At("RETURN"), remap = false)
    private static void axial_cosmetics$removeFissureHighlights(CallbackInfoReturnable<Object> cir) {
        FissureHighlightRemoval.disable();
    }
}
