package com.axial.cosmetics.mixin;

import com.axial.cosmetics.AxialCosmetics;
import com.axial.cosmetics.client.WaypointFeatureConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;

@Mixin(targets = "org.axial.axialutils.client.WaypointSettingsScreen", remap = false)
public abstract class WaypointSettingsScreenMixin {
    @Unique private boolean axial_cosmetics$waitingForKey;
    @Unique private static Field axial_cosmetics$panelXField;
    @Unique private static Field axial_cosmetics$panelYField;

    @Inject(method = "method_25394", at = @At("RETURN"), remap = false)
    private void axial_cosmetics$drawFeatureControls(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        try {
            int x = axial_cosmetics$getPanelX() + 50;
            int y = axial_cosmetics$getPanelY() + 6;
            String enabledLabel = "WAYPOINTS: " + (WaypointFeatureConfig.isEnabled() ? "ENABLED" : "DISABLED");
            String keyLabel = axial_cosmetics$waitingForKey ? "PRESS A KEY..." : "KEYBIND: " + AxialCosmetics.getWaypointMenuKey().getBoundKeyLocalizedText().getString();
            axial_cosmetics$drawButton(context, x, y, 182, enabledLabel, WaypointFeatureConfig.isEnabled());
            axial_cosmetics$drawButton(context, x + 190, y, 222, keyLabel, axial_cosmetics$waitingForKey);
        } catch (ReflectiveOperationException ignored) {
            // Preserve the original submenu if its layout fields change.
        }
    }

    @Inject(method = "method_25402", at = @At("HEAD"), cancellable = true, remap = false)
    private void axial_cosmetics$clickFeatureControls(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        try {
            int x = axial_cosmetics$getPanelX() + 50;
            int y = axial_cosmetics$getPanelY() + 6;
            if (axial_cosmetics$contains(click, x, y, 182, 18)) {
                WaypointFeatureConfig.setEnabled(!WaypointFeatureConfig.isEnabled());
                cir.setReturnValue(true);
            } else if (axial_cosmetics$contains(click, x + 190, y, 222, 18)) {
                axial_cosmetics$waitingForKey = true;
                cir.setReturnValue(true);
            }
        } catch (ReflectiveOperationException ignored) { }
    }

    @Inject(method = "method_25404", at = @At("HEAD"), cancellable = true, remap = false)
    private void axial_cosmetics$rebindWaypointKey(KeyInput input, CallbackInfoReturnable<Boolean> cir) {
        if (!axial_cosmetics$waitingForKey) return;
        axial_cosmetics$waitingForKey = false;
        AxialCosmetics.getWaypointMenuKey().setBoundKey(InputUtil.fromKeyCode(input));
        MinecraftClient.getInstance().options.write();
        cir.setReturnValue(true);
    }

    @Unique
    private static boolean axial_cosmetics$contains(Click click, int x, int y, int width, int height) {
        return click.x() >= x && click.x() <= x + width && click.y() >= y && click.y() <= y + height;
    }

    @Unique
    private static void axial_cosmetics$drawButton(DrawContext context, int x, int y, int width, String text, boolean active) {
        context.fill(x, y, x + width, y + 18, active ? 0xFF3A7258 : 0xFF33363D);
        context.fill(x, y, x + width, y + 1, 0xFF8AA2B8);
        context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, text, x + width / 2, y + 5, 0xFFFFFFFF);
    }

    @Unique
    private int axial_cosmetics$getPanelX() throws ReflectiveOperationException {
        if (axial_cosmetics$panelXField == null) {
            axial_cosmetics$panelXField = this.getClass().getDeclaredField("panelX");
            axial_cosmetics$panelXField.setAccessible(true);
        }
        return axial_cosmetics$panelXField.getInt(this);
    }

    @Unique
    private int axial_cosmetics$getPanelY() throws ReflectiveOperationException {
        if (axial_cosmetics$panelYField == null) {
            axial_cosmetics$panelYField = this.getClass().getDeclaredField("panelY");
            axial_cosmetics$panelYField.setAccessible(true);
        }
        return axial_cosmetics$panelYField.getInt(this);
    }
}
