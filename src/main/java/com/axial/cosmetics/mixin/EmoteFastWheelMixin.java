package com.axial.cosmetics.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Uses Emotecraft's radial eight-slot chooser for the in-game fast menu.
 * The underlying setting is normally off by default, which produces a square grid.
 */
@Mixin(targets = "io.github.kosmx.emotes.arch.screen.widget.preview.PreviewFastChooseWidget", remap = false)
public abstract class EmoteFastWheelMixin {
    @ModifyExpressionValue(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lio/github/kosmx/emotes/common/SerializableConfig$ConfigEntry;get()Ljava/lang/Object;"
            ),
            remap = false
    )
    private Object axial_cosmetics$useRadialFastWheel(Object configuredValue) {
        return Boolean.TRUE;
    }
}
