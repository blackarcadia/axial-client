package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.QuickMenuScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Route every existing MAIN entry point after all injected mod entries have been built. */
@Mixin(targets = "org.axial.axialutils.client.AxialConfigScreen", remap = false)
public abstract class AxialConfigScreenModMenuMixin {
    @Inject(method = "rebuildLayout", at = @At("TAIL"), remap = false)
    private void axial_cosmetics$openModLibrary(CallbackInfo ci) {
        Screen backend = (Screen) (Object) this;
        MinecraftClient client = MinecraftClient.getInstance();
        client.send(() -> {
            if (client.currentScreen == backend) QuickMenuScreen.open(backend);
        });
    }
}
