package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.CrosshairConfigManager;
import com.axial.cosmetics.client.CrosshairDynamicState;
import com.axial.cosmetics.client.CrosshairRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class TitleOverlayCrosshairMixin {
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void axial_cosmetics$hideCrosshairInTitleOverlay(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        Screen currentScreen = MinecraftClient.getInstance().currentScreen;
        if (currentScreen != null && "org.axial.axialutils.client.TitleOverlaySettingsScreen".equals(currentScreen.getClass().getName())) {
            ci.cancel();
            return;
        }

        if (MinecraftClient.getInstance().player == null) {
            return;
        }

        if (CrosshairConfigManager.get().enabled) {
            int centerX = MinecraftClient.getInstance().getWindow().getScaledWidth() / 2;
            int centerY = MinecraftClient.getInstance().getWindow().getScaledHeight() / 2;
            float scaleMultiplier = CrosshairConfigManager.get().dynamicEnabled ? CrosshairDynamicState.scaleMultiplier() : 1.0f;
            CrosshairRenderer.render(context, centerX, centerY, CrosshairConfigManager.get(), scaleMultiplier);
            ci.cancel();
        }
    }
}
