package com.axial.cosmetics.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.axial.axialutils.client.AxialConfigManager;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.IntConsumer;

@Mixin(
        targets = {
                "org.axial.axialutils.client.AxialConfigScreen$MenuTile",
                "org.axial.axialutils.client.AxialConfigScreen$OptionRow",
                "org.axial.axialutils.client.HudColorSettingsScreen$ColorTile",
                "org.axial.axialutils.client.SatchelHelperColorSettingsScreen$ColorTile"
        },
        remap = false
)
public abstract class AxialColorTileMixin {

    @Inject(method = "activate", at = @At("HEAD"), cancellable = true, remap = false)
    private void axial_cosmetics$openColorPickerScreen(CallbackInfo ci) {
        try {
            Object colorGetter = axial_cosmetics$getOptionalField("colorGetter");
            Object colorSetter = axial_cosmetics$getOptionalField("colorSetter");
            if (colorGetter == null && !axial_cosmetics$hasField("colorGetter")) {
                colorGetter = axial_cosmetics$getOptionalField("getter");
            }
            if (colorSetter == null && !axial_cosmetics$hasField("colorSetter")) {
                colorSetter = axial_cosmetics$getOptionalField("setter");
            }
            if (colorGetter == null || colorSetter == null) {
                return;
            }

            Object screen = axial_cosmetics$getField("this$0");
            if (!(screen instanceof Screen parent)) {
                return;
            }

            String label = (String) axial_cosmetics$getField("label");
            int color = axial_cosmetics$getColor(colorGetter);
            Object resolvedColorSetter = colorSetter;
            IntConsumer onChange = value -> axial_cosmetics$setColor(resolvedColorSetter, value);
            MinecraftClient.getInstance().setScreen(new com.axial.cosmetics.client.CrosshairColorPickerScreen(parent, label, color, onChange, AxialConfigManager::save));
            ci.cancel();
        } catch (ReflectiveOperationException | ClassCastException | IllegalArgumentException ignored) {
            // Keep the base behavior if axialutils internals change.
        }
    }

    @Unique
    private Object axial_cosmetics$getField(String name) throws ReflectiveOperationException {
        Field field = this.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(this);
    }

    @Unique
    private Object axial_cosmetics$getOptionalField(String name) throws ReflectiveOperationException {
        if (!axial_cosmetics$hasField(name)) {
            return null;
        }
        return axial_cosmetics$getField(name);
    }

    @Unique
    private boolean axial_cosmetics$hasField(String name) {
        try {
            this.getClass().getDeclaredField(name);
            return true;
        } catch (NoSuchFieldException ignored) {
            return false;
        }
    }

    @Unique
    private int axial_cosmetics$getColor(Object colorGetter) throws ReflectiveOperationException {
        Method method = colorGetter.getClass().getDeclaredMethod("get");
        method.setAccessible(true);
        return (Integer) method.invoke(colorGetter);
    }

    @Unique
    private static void axial_cosmetics$setColor(Object colorSetter, int value) {
        try {
            Method method = colorSetter.getClass().getDeclaredMethod("set", int.class);
            method.setAccessible(true);
            method.invoke(colorSetter, value);
            AxialConfigManager.save();
        } catch (ReflectiveOperationException ignored) {
            // Ignore failed live updates rather than crashing the screen.
        }
    }
}
