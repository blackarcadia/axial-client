package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.HudColorDefaults;
import org.axial.axialutils.client.AxialConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Supplies the requested colors when a new Axial HUD configuration is created. */
@Mixin(AxialConfig.class)
public abstract class AxialConfigHudColorDefaultsMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void axial_cosmetics$setHudColorDefaults(CallbackInfo ci) {
        AxialConfig config = (AxialConfig) (Object) this;
        config.informationHudTitleColor = HudColorDefaults.TITLE;
        config.informationHudChargeColor = HudColorDefaults.SUBTITLE;
        config.informationHudXpColor = HudColorDefaults.SUBTITLE;
        config.satchelHudTitleColor = HudColorDefaults.TITLE;
        config.satchelHudCountColor = HudColorDefaults.SUBTITLE;
        config.satchelHudEmptyColor = HudColorDefaults.SUBTITLE;
        config.cpsHudTitleColor = HudColorDefaults.TITLE;
        config.cpsHudColor = HudColorDefaults.SUBTITLE;
        config.armorHudColor = HudColorDefaults.TITLE;
        config.armorHudDurabilityColor = HudColorDefaults.SUBTITLE;
    }
}
