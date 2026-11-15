package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.PotionsHudRenderer;
import com.axial.cosmetics.client.PotionsHudConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudPotionsHudMixin {
    @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"), cancellable = true)
    private void axial_cosmetics$hideVanillaPotionIcons(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (PotionsHudConfig.isEnabled()) ci.cancel();
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void axial_cosmetics$renderPotionsHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        PotionsHudRenderer.render(context);
    }
}
