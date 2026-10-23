package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.ChunkBordersSettingsScreen;
import com.axial.cosmetics.client.CompassSettingsScreen;
import com.axial.cosmetics.client.CrosshairSettingsScreen;
import com.axial.cosmetics.client.ItemScalerSettingsScreen;
import net.minecraft.client.gui.Click;
import org.axial.axialutils.client.AxialUiTheme;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Legacy screens draw their controls manually and need a click-sound hook.
 * Navigation screens use native widgets with their own sounds and inherit mouseClicked.
 */
@Mixin({
        ChunkBordersSettingsScreen.class,
        CompassSettingsScreen.class,
        CrosshairSettingsScreen.class,
        ItemScalerSettingsScreen.class
})
public abstract class CustomSubmenuClickSoundMixin {
    @Inject(method = "mouseClicked", at = @At("RETURN"))
    private void axial_cosmetics$playStandardClickSound(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (click.button() == 0 && cir.getReturnValue()) {
            AxialUiTheme.playButtonClickSound();
        }
    }
}
