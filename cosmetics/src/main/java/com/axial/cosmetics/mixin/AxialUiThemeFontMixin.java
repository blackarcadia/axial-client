package com.axial.cosmetics.mixin;

import net.minecraft.text.Text;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "org.axial.axialutils.client.AxialUiTheme", remap = false)
public abstract class AxialUiThemeFontMixin {
    private static final StyleSpriteSource.Font UI_FONT = new StyleSpriteSource.Font(Identifier.of("axialutils", "ui_clean"));
    private static final String[] MENU_SCREEN_CLASSES = {
            "org.axial.axialutils.client.AxialConfigScreen",
            "org.axial.axialutils.client.HudColorPickerScreen",
            "org.axial.axialutils.client.HudColorSettingsScreen",
            "org.axial.axialutils.client.HudTitleRenamerScreen",
            "org.axial.axialutils.client.SatchelHelperColorSettingsScreen",
            "org.axial.axialutils.client.TitleOverlaySettingsScreen",
            "org.axial.axialutils.client.WaypointSettingsScreen",
            "com.axial.cosmetics.client.CosmeticMenuScreen"
    };
    private static final String[] ON_SCREEN_RENDERER_CLASSES = {
            "org.axial.axialutils.client.ArmorHudRenderer",
            "org.axial.axialutils.client.AxialHudRenderer",
            "org.axial.axialutils.client.CpsHudRenderer",
            "org.axial.axialutils.client.SatchelHudRenderer",
            "org.axial.axialutils.client.WaypointOverlayRenderer"
    };

    @Inject(method = "uiText", at = @At("RETURN"), cancellable = true)
    private static void axial_cosmetics$applyUiFont(String value, CallbackInfoReturnable<Text> cir) {
        if (isMenuScreenText()) {
            cir.setReturnValue(cir.getReturnValue().copy().styled(style -> style.withFont(UI_FONT)));
        }
    }

    @Inject(method = "uiBoldText", at = @At("RETURN"), cancellable = true)
    private static void axial_cosmetics$applyBoldUiFont(String value, CallbackInfoReturnable<Text> cir) {
        if (isMenuScreenText()) {
            cir.setReturnValue(cir.getReturnValue().copy().styled(style -> style.withFont(UI_FONT)));
        }
    }

    private static boolean isMenuScreenText() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (StackTraceElement frame : stack) {
            String className = frame.getClassName();
            for (String onScreenRendererClass : ON_SCREEN_RENDERER_CLASSES) {
                if (className.equals(onScreenRendererClass) || className.startsWith(onScreenRendererClass + "$")) {
                    return false;
                }
            }
        }

        for (StackTraceElement frame : stack) {
            String className = frame.getClassName();
            for (String menuScreenClass : MENU_SCREEN_CLASSES) {
                if (className.equals(menuScreenClass) || className.startsWith(menuScreenClass + "$")) {
                    return true;
                }
            }
        }
        return false;
    }
}
