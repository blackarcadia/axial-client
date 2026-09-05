package com.axial.cosmetics.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "org.axial.axialutils.client.SatchelHelperColorSettingsScreen$ColorTile", remap = false)
public abstract class SatchelHelperColorTileMixin {

    @Inject(method = "activate", at = @At("HEAD"), cancellable = true, remap = false)
    private void axial_cosmetics$disableSatchelColorPicker(CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true, remap = false)
    private void axial_cosmetics$hideSatchelColorPickerButton(CallbackInfo ci) {
        ci.cancel();
    }
}
