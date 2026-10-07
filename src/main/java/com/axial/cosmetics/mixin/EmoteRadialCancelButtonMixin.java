package com.axial.cosmetics.mixin;

import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Removes the redundant Cancel button from the hold-to-open quick wheel. */
@Mixin(targets = "io.github.kosmx.emotes.arch.screen.ingame.FastMenuScreen", remap = false)
public abstract class EmoteRadialCancelButtonMixin {
    @Inject(method = "init", at = @At("TAIL"), remap = false)
    private void axial_cosmetics$removeCancelButton(CallbackInfo ci) {
        // The screen's drawable list is owned by Screen; this reflection-free removal
        // keeps every other quick-wheel control intact.
        ((net.minecraft.client.gui.screen.Screen) (Object) this).children().removeIf(element ->
                element instanceof ButtonWidget button
                        && "Cancel".equalsIgnoreCase(button.getMessage().getString()));
    }
}
