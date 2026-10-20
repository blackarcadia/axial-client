package com.axial.cosmetics.mixin;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Keeps picker rows focused on the emote name and icon. */
@Mixin(targets = "io.github.kosmx.emotes.arch.gui.widgets.EmoteListWidget$ListEntry", remap = false)
public abstract class EmoteListDescriptionVisibilityMixin {
    @Redirect(
            method = "method_25343",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/class_332;method_27535(Lnet/minecraft/class_327;Lnet/minecraft/class_2561;III)V"),
            remap = false
    )
    private void axial_cosmetics$hideDescription(DrawContext context, TextRenderer textRenderer, Text description,
                                                  int x, int y, int color) {
        // Metadata remains available to Emotecraft for search and playback;
        // only its secondary line is omitted from the picker row.
    }
}
