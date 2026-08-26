package com.axial.cosmetics.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientTitleMixin {
    private static final String AXIAL_WINDOW_TITLE = "AxialClient Version 1.0 Alphatest";

    @Inject(method = "getWindowTitle", at = @At("RETURN"), cancellable = true)
    private void axial_cosmetics$replaceWindowTitle(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(AXIAL_WINDOW_TITLE);
    }
}
