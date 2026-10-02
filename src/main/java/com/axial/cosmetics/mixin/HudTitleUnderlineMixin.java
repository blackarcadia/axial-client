package com.axial.cosmetics.mixin;

import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Underlines the headings rendered by the bundled HUDs. */
@Mixin(
        targets = {
                "org.axial.axialutils.client.AxialHudRenderer",
                "org.axial.axialutils.client.SatchelHudRenderer",
                "org.axial.axialutils.client.CpsHudRenderer",
                "org.axial.axialutils.client.ArmorHudRenderer"
        },
        remap = false
)
public abstract class HudTitleUnderlineMixin {
    @Inject(method = "titleText", at = @At("RETURN"), cancellable = true, remap = false)
    private static void axial_cosmetics$underlineHudTitle(CallbackInfoReturnable<Text> cir) {
        cir.setReturnValue(cir.getReturnValue().copy().styled(style -> style.withUnderline(true)));
    }
}
