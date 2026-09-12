package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.CompassConfigAccess;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets = "org.axial.axialutils.client.AxialConfig", remap = false)
public abstract class AxialConfigCompassMixin implements CompassConfigAccess {
    public Boolean showCompass = Boolean.TRUE;
    public Integer compassX;
    public Integer compassY;

    @Override
    public Integer axial_cosmetics$getCompassX() {
        return compassX;
    }

    @Override
    public Integer axial_cosmetics$getCompassY() {
        return compassY;
    }

    @Override
    public void axial_cosmetics$setCompassPosition(int x, int y) {
        compassX = x;
        compassY = y;
    }


    @Override
    public Boolean axial_cosmetics$getShowCompass() {
        return showCompass;
    }

    @Override
    public void axial_cosmetics$setShowCompass(Boolean showCompass) {
        this.showCompass = showCompass;
    }
}
