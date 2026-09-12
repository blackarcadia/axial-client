package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.ModMenuBackButton;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "org.axial.axialutils.client.AxialUiTheme", remap = false)
public abstract class ModMenuBackButtonMixin {
    @Inject(method = "drawButton", at = @At("HEAD"), cancellable = true)
    private static void axial_cosmetics$drawSharedBackButton(
            DrawContext context, TextRenderer textRenderer, int x, int y, int width, int height,
            String label, String subtitle, boolean hovered, boolean pressed, int accent, CallbackInfo ci) {
        if ("BACK".equalsIgnoreCase(label) || "<".equals(label) || "<-".equals(label)) {
            ModMenuBackButton.draw(context, x, y, width, height, hovered || pressed);
            ci.cancel();
        }
    }
}
