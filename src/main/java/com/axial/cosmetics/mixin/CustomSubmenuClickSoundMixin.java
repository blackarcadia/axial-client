package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.ChunkBordersSettingsScreen;
import com.axial.cosmetics.client.CompassSettingsScreen;
import com.axial.cosmetics.client.CreateWaypointScreen;
import com.axial.cosmetics.client.CrosshairColorPickerScreen;
import com.axial.cosmetics.client.CrosshairSettingsScreen;
import com.axial.cosmetics.client.ItemScalerSettingsScreen;
import com.axial.cosmetics.client.WaypointSettingsScreen;
import net.minecraft.client.gui.Click;
import org.axial.axialutils.client.AxialUiTheme;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Applies AxialUtils' standard press sound to controls drawn by our custom screens. */
@Mixin({
        ChunkBordersSettingsScreen.class,
        CompassSettingsScreen.class,
        CreateWaypointScreen.class,
        CrosshairColorPickerScreen.class,
        CrosshairSettingsScreen.class,
        ItemScalerSettingsScreen.class,
        WaypointSettingsScreen.class
})
public abstract class CustomSubmenuClickSoundMixin {
    @Inject(method = "mouseClicked", at = @At("RETURN"))
    private void axial_cosmetics$playStandardClickSound(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (click.button() == 0 && cir.getReturnValue()) {
            AxialUiTheme.playButtonClickSound();
        }
    }
}
