package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.CompassConfigAccess;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets = "org.axial.axialutils.client.AxialConfig", remap = false)
public abstract class AxialConfigCompassMixin implements CompassConfigAccess {
    public Boolean showCompass = Boolean.TRUE;

    @Override
    public Boolean axial_cosmetics$getShowCompass() {
        return showCompass;
    }

    @Override
    public void axial_cosmetics$setShowCompass(Boolean showCompass) {
        this.showCompass = showCompass;
    }
}
