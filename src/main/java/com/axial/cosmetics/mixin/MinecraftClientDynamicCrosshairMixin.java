package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.CrosshairConfigManager;
import com.axial.cosmetics.client.CrosshairDynamicState;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientDynamicCrosshairMixin {
    @Inject(method = "doAttack", at = @At("HEAD"))
    private void axial_cosmetics$triggerDynamicCrosshairOnAttack(CallbackInfoReturnable<Boolean> cir) {
        axial_cosmetics$triggerDynamicCrosshair();
    }

    @Inject(method = "doItemUse", at = @At("HEAD"))
    private void axial_cosmetics$triggerDynamicCrosshairOnUse(CallbackInfo ci) {
        axial_cosmetics$triggerDynamicCrosshair();
    }

    private void axial_cosmetics$triggerDynamicCrosshair() {
        MinecraftClient client = (MinecraftClient) (Object) this;
        if (client.player == null || client.currentScreen != null) {
            return;
        }

        if (CrosshairConfigManager.get().enabled && CrosshairConfigManager.get().dynamicEnabled) {
            CrosshairDynamicState.triggerPulse();
        }
    }
}
