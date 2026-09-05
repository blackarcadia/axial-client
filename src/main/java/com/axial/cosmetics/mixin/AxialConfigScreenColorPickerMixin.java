package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.CrosshairColorPickerScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.axial.axialutils.client.AxialConfigManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.IntConsumer;

@Mixin(targets = "org.axial.axialutils.client.AxialConfigScreen", remap = false)
public abstract class AxialConfigScreenColorPickerMixin {
    @Unique
    private static Field axial_cosmetics$colorPickerLabelField;
    @Unique
    private static Field axial_cosmetics$colorPickerGetterField;
    @Unique
    private static Field axial_cosmetics$colorPickerSetterField;
    @Unique
    private static Field axial_cosmetics$colorPickerGroupField;
    @Unique
    private static Field axial_cosmetics$colorPickerTargetField;
    @Unique
    private static Field axial_cosmetics$activeColorEditorField;

    @Redirect(
            method = "method_25402",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/axial/axialutils/client/AxialConfigScreen$MenuTile;activate()V",
                    remap = false
            ),
            remap = false
    )
    private void axial_cosmetics$openTextureBackedColorPicker(@Coerce Object tile) {
        if (axial_cosmetics$tryOpenColorPicker(tile)) {
            return;
        }
        axial_cosmetics$activateTile(tile);
    }

    @Unique
    private boolean axial_cosmetics$tryOpenColorPicker(Object tile) {
        try {
            Object group = axial_cosmetics$getColorGroup(tile);
            Object target = axial_cosmetics$getColorTarget(tile);
            if (group == null || target == null) {
                return false;
            }

            String label = (String) axial_cosmetics$getLabel(tile);
            Object colorGetter = axial_cosmetics$getColorGetter(tile);
            Object colorSetter = axial_cosmetics$getColorSetter(tile);
            if (colorGetter == null || colorSetter == null) {
                return false;
            }

            int color = axial_cosmetics$getColor(colorGetter);
            Object resolvedColorSetter = colorSetter;
            IntConsumer onChange = value -> axial_cosmetics$setColor(resolvedColorSetter, value);
            axial_cosmetics$clearInlineColorEditor();
            MinecraftClient.getInstance().setScreen(
                    new CrosshairColorPickerScreen((Screen) (Object) this, label, color, onChange, AxialConfigManager::save)
            );
            return true;
        } catch (ReflectiveOperationException | ClassCastException ignored) {
            return false;
        }
    }

    @Unique
    private static void axial_cosmetics$activateTile(Object tile) {
        try {
            Method method = tile.getClass().getDeclaredMethod("activate");
            method.setAccessible(true);
            method.invoke(tile);
        } catch (ReflectiveOperationException ignored) {
            // Leave the click consumed if axialutils internals change.
        }
    }

    @Unique
    private static Object axial_cosmetics$getLabel(Object tile) throws ReflectiveOperationException {
        if (axial_cosmetics$colorPickerLabelField == null) {
            axial_cosmetics$colorPickerLabelField = tile.getClass().getDeclaredField("label");
            axial_cosmetics$colorPickerLabelField.setAccessible(true);
        }
        return axial_cosmetics$colorPickerLabelField.get(tile);
    }

    @Unique
    private static Object axial_cosmetics$getColorGetter(Object tile) throws ReflectiveOperationException {
        if (axial_cosmetics$colorPickerGetterField == null) {
            axial_cosmetics$colorPickerGetterField = tile.getClass().getDeclaredField("colorGetter");
            axial_cosmetics$colorPickerGetterField.setAccessible(true);
        }
        return axial_cosmetics$colorPickerGetterField.get(tile);
    }

    @Unique
    private static Object axial_cosmetics$getColorSetter(Object tile) throws ReflectiveOperationException {
        if (axial_cosmetics$colorPickerSetterField == null) {
            axial_cosmetics$colorPickerSetterField = tile.getClass().getDeclaredField("colorSetter");
            axial_cosmetics$colorPickerSetterField.setAccessible(true);
        }
        return axial_cosmetics$colorPickerSetterField.get(tile);
    }

    @Unique
    private static Object axial_cosmetics$getColorGroup(Object tile) throws ReflectiveOperationException {
        if (axial_cosmetics$colorPickerGroupField == null) {
            axial_cosmetics$colorPickerGroupField = tile.getClass().getDeclaredField("colorGroup");
            axial_cosmetics$colorPickerGroupField.setAccessible(true);
        }
        return axial_cosmetics$colorPickerGroupField.get(tile);
    }

    @Unique
    private static Object axial_cosmetics$getColorTarget(Object tile) throws ReflectiveOperationException {
        if (axial_cosmetics$colorPickerTargetField == null) {
            axial_cosmetics$colorPickerTargetField = tile.getClass().getDeclaredField("colorTarget");
            axial_cosmetics$colorPickerTargetField.setAccessible(true);
        }
        return axial_cosmetics$colorPickerTargetField.get(tile);
    }

    @Unique
    private static int axial_cosmetics$getColor(Object colorGetter) throws ReflectiveOperationException {
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

    @Unique
    private void axial_cosmetics$clearInlineColorEditor() throws ReflectiveOperationException {
        if (axial_cosmetics$activeColorEditorField == null) {
            axial_cosmetics$activeColorEditorField = this.getClass().getDeclaredField("activeColorEditor");
            axial_cosmetics$activeColorEditorField.setAccessible(true);
        }
        axial_cosmetics$activeColorEditorField.set(this, null);
    }
}
