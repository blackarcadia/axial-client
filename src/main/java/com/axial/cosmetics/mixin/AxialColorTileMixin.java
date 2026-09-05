package com.axial.cosmetics.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.axial.axialutils.client.AxialConfigManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.IntConsumer;

@Mixin(targets = "org.axial.axialutils.client.AxialConfigScreen$MenuTile", remap = false)
public abstract class AxialColorTileMixin {
    private static Field axial_cosmetics$screenField;
    private static Field axial_cosmetics$labelField;
    private static Field axial_cosmetics$colorGetterField;
    private static Field axial_cosmetics$colorSetterField;

    @Inject(method = "activate", at = @At("HEAD"), cancellable = true, remap = false)
    private void axial_cosmetics$openColorPickerScreen(CallbackInfo ci) {
        try {
            Object colorGetter = axial_cosmetics$getField("colorGetter", axial_cosmetics$colorGetterField);
            Object colorSetter = axial_cosmetics$getField("colorSetter", axial_cosmetics$colorSetterField);
            if (colorGetter == null || colorSetter == null) {
                return;
            }

            Object screen = axial_cosmetics$getField("this$0", axial_cosmetics$screenField);
            if (!(screen instanceof Screen parent)) {
                return;
            }

            String label = (String) axial_cosmetics$getField("label", axial_cosmetics$labelField);
            int color = axial_cosmetics$getColor(colorGetter);
            IntConsumer onChange = value -> axial_cosmetics$setColor(colorSetter, value);
            MinecraftClient.getInstance().setScreen(new com.axial.cosmetics.client.CrosshairColorPickerScreen(parent, label, color, onChange, AxialConfigManager::save));
            ci.cancel();
        } catch (ReflectiveOperationException ignored) {
            // Keep the base inline editor behavior if axialutils internals change.
        }
    }

    private Object axial_cosmetics$getField(String name, Field cachedField) throws ReflectiveOperationException {
        Field field = cachedField;
        if (field == null) {
            field = this.getClass().getDeclaredField(name);
            field.setAccessible(true);
            if ("this$0".equals(name)) {
                axial_cosmetics$screenField = field;
            } else if ("label".equals(name)) {
                axial_cosmetics$labelField = field;
            } else if ("colorGetter".equals(name)) {
                axial_cosmetics$colorGetterField = field;
            } else if ("colorSetter".equals(name)) {
                axial_cosmetics$colorSetterField = field;
            }
        }
        return field.get(this);
    }

    private int axial_cosmetics$getColor(Object colorGetter) throws ReflectiveOperationException {
        Method method = colorGetter.getClass().getDeclaredMethod("get");
        method.setAccessible(true);
        return (Integer) method.invoke(colorGetter);
    }

    private static void axial_cosmetics$setColor(Object colorSetter, int value) {
        try {
            Method method = colorSetter.getClass().getDeclaredMethod("set", int.class);
            method.setAccessible(true);
            method.invoke(colorSetter, value);
        } catch (ReflectiveOperationException ignored) {
            // Ignore failed live updates rather than crashing the screen.
        }
    }
}
