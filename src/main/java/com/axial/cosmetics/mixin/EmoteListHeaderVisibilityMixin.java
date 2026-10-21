package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.AxialEmoteLoadoutScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Removes Emotecraft's redundant root-path breadcrumb from Axial's loadout. */
@Mixin(targets = "io.github.kosmx.emotes.arch.gui.widgets.EmoteListWidget$HeaderEntry", remap = false)
public abstract class EmoteListHeaderVisibilityMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void axial_cosmetics$hideRootBreadcrumb(DrawContext context, int mouseX, int mouseY,
                                                     boolean hovered, float delta, CallbackInfo ci) {
        if (MinecraftClient.getInstance().currentScreen instanceof AxialEmoteLoadoutScreen) {
            ci.cancel();
        }
    }
}
