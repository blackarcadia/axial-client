package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.EmotePermissionState;
import io.github.kosmx.emotes.main.EmoteHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Removes emotes not granted by the connected Axial server before the menu is built. */
@Mixin(targets = "io.github.kosmx.emotes.arch.gui.widgets.EmoteListWidget", remap = false)
public abstract class EmoteListWidgetPermissionMixin {
    @ModifyVariable(method = "setEmotes", at = @At("HEAD"), argsOnly = true, ordinal = 0, remap = false)
    private Iterable<EmoteHolder> axial_cosmetics$filterUnavailableEmotes(Iterable<EmoteHolder> emotes) {
        return EmotePermissionState.filter(emotes);
    }
}
