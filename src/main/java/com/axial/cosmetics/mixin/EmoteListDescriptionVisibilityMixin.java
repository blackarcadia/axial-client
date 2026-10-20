package com.axial.cosmetics.mixin;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps picker rows focused on the emote name and icon. */
@Mixin(targets = "io.github.kosmx.emotes.arch.gui.widgets.EmoteListWidget$ListEntry", remap = false)
public abstract class EmoteListDescriptionVisibilityMixin {
    /**
     * Uses the same, already-released list-entry render hook as the metadata
     * suppression below.  It only paints behind Emotecraft's own icon and
     * text; selection, scrolling and click handling remain untouched.
     */
    @Inject(method = "method_25343", at = @At("HEAD"), remap = false, require = 0)
    private void axial_cosmetics$drawEmoteCard(DrawContext context, int x, int y, boolean selected,
                                                float delta, CallbackInfo ci) {
        int cardWidth = 258;
        int cardHeight = 34;
        int fill = selected ? 0xE62B2040 : 0xD9161426;
        int border = selected ? 0xFFB84CFF : 0x804D3E70;

        context.fill(x - 4, y - 2, x + cardWidth, y + cardHeight, fill);
        context.fill(x - 3, y - 1, x + cardWidth - 1, y, 0x32FFFFFF);
        context.drawStrokedRectangle(x - 4, y - 2, cardWidth + 4, cardHeight + 2, border);
        if (selected) {
            context.fill(x - 4, y - 2, x - 1, y + cardHeight, 0xFFCF4BFF);
        }
    }

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
