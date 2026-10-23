package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.QuickMenuScreen;
import com.axial.cosmetics.client.ClientSettingsScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Route MAIN and OPTIONS after initialization, before a legacy menu frame can render. */
@Mixin(MinecraftClient.class)
public abstract class AxialConfigScreenModMenuMixin {
    @Inject(method = "setScreen", at = @At("TAIL"))
    private void axial_cosmetics$openModLibrary(Screen screen, CallbackInfo ci) {
        MinecraftClient client = (MinecraftClient) (Object) this;
        if (screen == null || client.currentScreen != screen
                || !screen.getClass().getName().equals("org.axial.axialutils.client.AxialConfigScreen")) return;
        // All layout injections have completed. Replacing the screen here also avoids
        // re-entering setScreen during init, where the outer call could overwrite it.
        if(!ClientSettingsScreen.openOptions(screen))QuickMenuScreen.open(screen);
    }
}
