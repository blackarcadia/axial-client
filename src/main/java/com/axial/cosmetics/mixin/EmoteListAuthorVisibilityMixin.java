package com.axial.cosmetics.mixin;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Hides Emotecraft's author line while retaining the picker icon and emote name. */
@Mixin(targets = "io.github.kosmx.emotes.arch.gui.widgets.EmoteListWidget$EmoteEntry", remap = false)
public abstract class EmoteListAuthorVisibilityMixin {
    @Redirect(
            method = "renderAdditional",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/class_332;method_27535(Lnet/minecraft/class_327;Lnet/minecraft/class_2561;III)V"),
            remap = false
    )
    private void axial_cosmetics$hideAuthor(DrawContext context, TextRenderer textRenderer, Text author,
                                             int x, int y, int color) {
        // The subsequent icon render still runs normally.
    }
}
