package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.EmoteMenuTheme;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Recolours native selection frames without replacing list selection or scrolling. */
@Mixin(targets="io.github.kosmx.emotes.arch.gui.widgets.EmoteListWidget",remap=false)
public abstract class EmoteListPaletteMixin {
    @ModifyVariable(method="renderSelection",at=@At("HEAD"),argsOnly=true,ordinal=0,remap=false)
    private int axial$selectedColour(int original){return EmoteMenuTheme.BRAND;}
}
