package com.axial.cosmetics.mixin;

import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Removes the isolated one-pixel corner decorations from theme buttons. */
@Mixin(targets = "org.axial.axialutils.client.AxialUiTheme", remap = false)
public abstract class AxialUiThemeNoCornerPixelsMixin {
    @Redirect(
            method = {"drawButton", "drawIconButton"},
            at = @At(value = "INVOKE", target = "Lorg/axial/axialutils/client/AxialUiTheme;drawCorner(Lnet/minecraft/client/gui/DrawContext;IIZZI)V"),
            remap = false,
            require = 0
    )
    private static void removeButtonCornerPixel(DrawContext context, int x, int y, boolean top, boolean left, int color) {
        // The border remains; only the disconnected corner pixel is omitted.
    }
}
