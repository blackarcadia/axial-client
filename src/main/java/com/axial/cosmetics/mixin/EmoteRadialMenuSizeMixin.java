package com.axial.cosmetics.mixin;

import io.github.kosmx.emotes.PlatformTools;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Scales the legacy radial chooser as one unit so previews and hitboxes stay aligned. */
@Mixin(targets = "io.github.kosmx.emotes.arch.screen.ingame.FastMenuScreen", remap = false)
public abstract class EmoteRadialMenuSizeMixin {
    @ModifyArg(
            method = "method_48640",
            at = @At(value = "INVOKE", target = "Lio/github/kosmx/emotes/arch/screen/widget/AbstractFastChooseWidget;setSize(I)V"),
            index = 0,
            remap = false
    )
    private int axial_cosmetics$compactRadialWheel(int originalSize) {
        if (!Boolean.TRUE.equals(PlatformTools.getConfig().oldChooseWheel.get())) return originalSize;
        return Math.max(280, Math.round(originalSize * 0.72f));
    }
}
