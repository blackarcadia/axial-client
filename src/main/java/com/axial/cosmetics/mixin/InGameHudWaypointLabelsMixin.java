package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.WaypointLabelRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudWaypointLabelsMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void axial_cosmetics$renderWaypointLabels(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        WaypointLabelRenderer.render(context);
    }
}
