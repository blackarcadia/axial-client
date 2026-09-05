package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.FissureHighlightRemoval;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.List;

@Mixin(targets = "org.axial.axialutils.client.AxialConfigScreen", remap = false)
public abstract class AxialConfigScreenFissureRemovalMixin {
    @Unique
    private static Field axial_cosmetics$fissureRemovalModeField;
    @Unique
    private static Field axial_cosmetics$fissureRemovalTilesField;
    @Unique
    private static Field axial_cosmetics$fissureRemovalTileLabelField;

    @Inject(method = "rebuildLayout", at = @At("HEAD"), remap = false, order = 980)
    private void axial_cosmetics$redirectFissureScreen(CallbackInfo ci) {
        try {
            if ("FISSURE".equals(axial_cosmetics$fissureRemovalGetMode().toString())) {
                axial_cosmetics$fissureRemovalSetMode("MAIN");
            }
        } catch (ReflectiveOperationException | ClassCastException ignored) {
            // Leave the upstream screen alone if its internals change.
        }
    }

    @Inject(method = "rebuildLayout", at = @At("RETURN"), remap = false, order = 980)
    private void axial_cosmetics$removeFissureHighlightsTile(CallbackInfo ci) {
        FissureHighlightRemoval.disable();
        try {
            Object mode = axial_cosmetics$fissureRemovalGetMode();
            if (mode == null || !"MAIN".equals(mode.toString())) {
                return;
            }

            Object rawTiles = axial_cosmetics$fissureRemovalGetTiles();
            if (!(rawTiles instanceof List<?> tiles)) {
                return;
            }

            @SuppressWarnings("unchecked")
            List<Object> mutableTiles = (List<Object>) tiles;
            mutableTiles.removeIf(tile -> {
                try {
                    String label = axial_cosmetics$fissureRemovalGetTileLabel(tile);
                    return "FISSURE HIGHLIGHTS".equals(label) || "RUPTURE HIGHLIGHTS".equals(label);
                } catch (ReflectiveOperationException ignored) {
                    return false;
                }
            });
        } catch (ReflectiveOperationException | ClassCastException ignored) {
            // Keep the upstream menu usable if its private layout details change.
        }
    }

    @Unique
    private Object axial_cosmetics$fissureRemovalGetMode() throws ReflectiveOperationException {
        if (axial_cosmetics$fissureRemovalModeField == null) {
            axial_cosmetics$fissureRemovalModeField = this.getClass().getDeclaredField("mode");
            axial_cosmetics$fissureRemovalModeField.setAccessible(true);
        }
        return axial_cosmetics$fissureRemovalModeField.get(this);
    }

    @Unique
    private Object axial_cosmetics$fissureRemovalGetTiles() throws ReflectiveOperationException {
        if (axial_cosmetics$fissureRemovalTilesField == null) {
            axial_cosmetics$fissureRemovalTilesField = this.getClass().getDeclaredField("tiles");
            axial_cosmetics$fissureRemovalTilesField.setAccessible(true);
        }
        return axial_cosmetics$fissureRemovalTilesField.get(this);
    }

    @Unique
    private void axial_cosmetics$fissureRemovalSetMode(String modeName) throws ReflectiveOperationException {
        Object currentMode = axial_cosmetics$fissureRemovalGetMode();
        if (currentMode == null) {
            return;
        }

        Class<?> modeClass = currentMode.getClass();
        @SuppressWarnings({"rawtypes", "unchecked"})
        Object mainMode = Enum.valueOf((Class<? extends Enum>) modeClass, modeName);
        if (axial_cosmetics$fissureRemovalModeField == null) {
            axial_cosmetics$fissureRemovalModeField = this.getClass().getDeclaredField("mode");
            axial_cosmetics$fissureRemovalModeField.setAccessible(true);
        }
        axial_cosmetics$fissureRemovalModeField.set(this, mainMode);
    }

    @Unique
    private static String axial_cosmetics$fissureRemovalGetTileLabel(Object tile) throws ReflectiveOperationException {
        if (axial_cosmetics$fissureRemovalTileLabelField == null) {
            axial_cosmetics$fissureRemovalTileLabelField = tile.getClass().getDeclaredField("label");
            axial_cosmetics$fissureRemovalTileLabelField.setAccessible(true);
        }
        return (String) axial_cosmetics$fissureRemovalTileLabelField.get(tile);
    }
}
