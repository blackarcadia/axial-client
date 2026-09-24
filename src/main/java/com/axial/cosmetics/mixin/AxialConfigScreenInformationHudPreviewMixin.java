package com.axial.cosmetics.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.axial.axialutils.client.AxialConfigManager;
import org.axial.axialutils.client.AxialHudRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "org.axial.axialutils.client.AxialConfigScreen", remap = false)
public abstract class AxialConfigScreenInformationHudPreviewMixin {
    @Redirect(
            method = "method_25394",
            at = @At(value = "INVOKE", target =
                    "Lorg/axial/axialutils/client/AxialHudRenderer;renderPreview(Lnet/minecraft/class_332;Lnet/minecraft/class_310;II)V",
                    remap = false),
            remap = false
    )
    private static void axial_cosmetics$renderInformationHudPreviewWhenEnabled(
            DrawContext context, MinecraftClient client, int x, int y) {
        if (AxialConfigManager.get().hudEnabled) {
            AxialHudRenderer.renderPreview(context, client, x, y);
        }
    }
}
