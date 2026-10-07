package com.axial.cosmetics.mixin;

import io.github.kosmx.emotes.PlatformTools;
import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hides Emotecraft's outer keyboard-slot numbers; Axial draws its own compact labels. */
@Mixin(targets = "io.github.kosmx.emotes.arch.screen.widget.preview.elemets.PlayerChooseElement", remap = false)
public abstract class EmoteRadialSlotNumberMixin {
    @Inject(method = "renderTileId", at = @At("HEAD"), cancellable = true, remap = false)
    private void axial_cosmetics$hideOuterSlotNumbers(DrawContext context, float animationProgress, CallbackInfo ci) {
        if (Boolean.TRUE.equals(PlatformTools.getConfig().oldChooseWheel.get())) {
            ci.cancel();
        }
    }
}
