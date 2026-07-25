package com.axial.cosmetics.mixin;

import net.minecraft.client.MinecraftClient;
import org.axial.axialutils.client.AxialConfigManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
        targets = {
                "org.axial.axialutils.client.AxialConfigScreen",
                "org.axial.axialutils.client.HudColorPickerScreen",
                "org.axial.axialutils.client.HudTitleRenamerScreen",
                "org.axial.axialutils.client.TitleOverlaySettingsScreen"
        },
        remap = false
)
public abstract class CloseModMenuMixin {
    @Inject(method = "method_25419", at = @At("HEAD"), cancellable = true, remap = false)
    private void axial_cosmetics$closeEntireModMenu(CallbackInfo ci) {
        AxialConfigManager.save();
        MinecraftClient.getInstance().setScreen(null);
        ci.cancel();
    }
}
