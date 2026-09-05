package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.CrosshairColorPickerScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.axial.axialutils.client.AxialConfigManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.function.IntConsumer;

@Mixin(targets = "org.axial.axialutils.client.AxialConfigScreen", remap = false)
public abstract class AxialConfigScreenColorPickerMixin {
    @Unique
    private static Field axial_cosmetics$tilesField;
    @Unique
    private static Field axial_cosmetics$activeColorEditorField;

    @Inject(method = "addColorRows", at = @At("HEAD"), cancellable = true, remap = false)
    private void axial_cosmetics$hideSatchelColorRows(int startX, int startY, @Coerce Object group, CallbackInfoReturnable<Integer> cir) {
        if (axial_cosmetics$removesColorSection(group)) {
            cir.setReturnValue(startY);
        }
    }

    @Inject(method = "rebuildLayout", at = @At("TAIL"), remap = false)
    private void axial_cosmetics$pruneRemovedColorTiles(CallbackInfo ci) {
        try {
            List<?> tiles = axial_cosmetics$getTiles();
            if (tiles == null) {
                return;
            }

            Iterator<?> iterator = tiles.iterator();
            while (iterator.hasNext()) {
                Object tile = iterator.next();
                String label = (String) axial_cosmetics$getLabel(tile);
                if (label != null && axial_cosmetics$isRemovedColorLabel(label)) {
                    iterator.remove();
                }
            }

            Object activeColorEditor = axial_cosmetics$getActiveColorEditor();
            if (activeColorEditor != null) {
                Object group = axial_cosmetics$getOptionalField(activeColorEditor, "group");
                if (axial_cosmetics$removesColorSection(group)) {
                    axial_cosmetics$clearActiveColorEditor();
                }
            }
        } catch (ReflectiveOperationException ignored) {
            // Leave the base layout alone if AxialUtils internals shift.
        }
    }

    @Redirect(
            method = "method_25402",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/axial/axialutils/client/AxialConfigScreen$MenuTile;activate()V",
                    remap = false
            ),
            remap = false
    )
    private void axial_cosmetics$openMenuTileColorPicker(@Coerce Object tile) {
        if (axial_cosmetics$tryOpenColorPicker(tile)) {
            return;
        }
        axial_cosmetics$activateTile(tile);
    }

    @Redirect(
            method = "method_25402",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/axial/axialutils/client/AxialConfigScreen$OptionRow;activate()V",
                    remap = false
            ),
            remap = false
    )
    private void axial_cosmetics$openOptionRowColorPicker(@Coerce Object tile) {
        if (axial_cosmetics$tryOpenColorPicker(tile)) {
            return;
        }
        axial_cosmetics$activateTile(tile);
    }

    @Unique
    private boolean axial_cosmetics$tryOpenColorPicker(Object tile) {
        try {
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
        } catch (ReflectiveOperationException | ClassCastException | IllegalArgumentException ignored) {
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
        return axial_cosmetics$getField(tile, "label");
    }

    @Unique
    private static Object axial_cosmetics$getColorGetter(Object tile) throws ReflectiveOperationException {
        Object colorGetter = axial_cosmetics$getOptionalField(tile, "colorGetter");
        return colorGetter != null || axial_cosmetics$hasField(tile, "colorGetter")
                ? colorGetter
                : axial_cosmetics$getOptionalField(tile, "getter");
    }

    @Unique
    private static Object axial_cosmetics$getColorSetter(Object tile) throws ReflectiveOperationException {
        Object colorSetter = axial_cosmetics$getOptionalField(tile, "colorSetter");
        return colorSetter != null || axial_cosmetics$hasField(tile, "colorSetter")
                ? colorSetter
                : axial_cosmetics$getOptionalField(tile, "setter");
    }

    @Unique
    private static Object axial_cosmetics$getField(Object instance, String name) throws ReflectiveOperationException {
        Field field = instance.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(instance);
    }

    @Unique
    private static Object axial_cosmetics$getOptionalField(Object instance, String name) throws ReflectiveOperationException {
        if (!axial_cosmetics$hasField(instance, name)) {
            return null;
        }
        return axial_cosmetics$getField(instance, name);
    }

    @Unique
    private List<?> axial_cosmetics$getTiles() throws ReflectiveOperationException {
        if (axial_cosmetics$tilesField == null) {
            axial_cosmetics$tilesField = this.getClass().getDeclaredField("tiles");
            axial_cosmetics$tilesField.setAccessible(true);
        }
        return (List<?>) axial_cosmetics$tilesField.get(this);
    }

    @Unique
    private Object axial_cosmetics$getActiveColorEditor() throws ReflectiveOperationException {
        if (axial_cosmetics$activeColorEditorField == null) {
            axial_cosmetics$activeColorEditorField = this.getClass().getDeclaredField("activeColorEditor");
            axial_cosmetics$activeColorEditorField.setAccessible(true);
        }
        return axial_cosmetics$activeColorEditorField.get(this);
    }

    @Unique
    private void axial_cosmetics$clearActiveColorEditor() throws ReflectiveOperationException {
        if (axial_cosmetics$activeColorEditorField == null) {
            axial_cosmetics$activeColorEditorField = this.getClass().getDeclaredField("activeColorEditor");
            axial_cosmetics$activeColorEditorField.setAccessible(true);
        }
        axial_cosmetics$activeColorEditorField.set(this, null);
    }

    @Unique
    private static boolean axial_cosmetics$removesColorSection(Object group) {
        if (group == null) {
            return false;
        }
        String name = group.toString();
        return "SATCHEL".equals(name) || "CPS".equals(name) || "ARMOR".equals(name);
    }

    @Unique
    private static boolean axial_cosmetics$isRemovedColorLabel(String label) {
        return label.startsWith("SATCHEL COLORS") || label.startsWith("CPS COLORS") || label.startsWith("ARMOR COLORS");
    }

    @Unique
    private static boolean axial_cosmetics$hasField(Object instance, String name) {
        try {
            instance.getClass().getDeclaredField(name);
            return true;
        } catch (NoSuchFieldException ignored) {
            return false;
        }
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
