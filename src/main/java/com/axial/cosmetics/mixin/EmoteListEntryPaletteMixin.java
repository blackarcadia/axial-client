package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.EmoteMenuTheme;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Only the native hover fill changes; icons, permission checks and row actions remain native. */
@Mixin(targets="io.github.kosmx.emotes.arch.gui.widgets.EmoteListWidget$ListEntry",remap=false)
public abstract class EmoteListEntryPaletteMixin {
    @ModifyArg(method="method_25343",at=@At(value="INVOKE",target="Lnet/minecraft/class_332;method_25294(IIIII)V"),index=4,remap=false)
    private int axial$hoverColour(int original){return EmoteMenuTheme.HOVER;}
}
