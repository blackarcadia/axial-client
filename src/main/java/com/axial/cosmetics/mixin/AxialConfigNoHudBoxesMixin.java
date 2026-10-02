package com.axial.cosmetics.mixin;

import org.axial.axialutils.client.AxialConfig;
import org.axial.axialutils.client.AxialConfigManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Disables the retired background boxes for every bundled HUD. */
@Mixin(AxialConfigManager.class)
public abstract class AxialConfigNoHudBoxesMixin {
    @Inject(method = "get", at = @At("RETURN"))
    private static void axial_cosmetics$disableHudBoxes(CallbackInfoReturnable<AxialConfig> cir) {
        AxialConfig config = cir.getReturnValue();
        config.showInformationHudBox = false;
        config.showSatchelsHudBox = false;
        config.showCpsBox = false;
        config.showArmorHudBox = false;
    }
}
