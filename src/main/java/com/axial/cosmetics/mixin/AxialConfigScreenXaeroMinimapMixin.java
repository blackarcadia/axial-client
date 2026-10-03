package com.axial.cosmetics.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/** Adds a shortcut to Xaero's native minimap settings beside Potions HUD. */
@Mixin(targets = "org.axial.axialutils.client.AxialConfigScreen", remap = false)
public abstract class AxialConfigScreenXaeroMinimapMixin {
    private static Field axial_cosmetics$modeField;
    private static Field axial_cosmetics$tilesField;
    private static Field axial_cosmetics$tileLabelField;
    private static Field axial_cosmetics$tileXField;
    private static Field axial_cosmetics$tileYField;
    private static Method axial_cosmetics$addActionTileMethod;

    @Inject(method = "rebuildLayout", at = @At("RETURN"), remap = false, order = 980)
    private void axial_cosmetics$addXaeroMinimapButton(CallbackInfo ci) {
        try {
            Object mode = axial_cosmetics$getMode();
            if (mode == null || !"MAIN".equals(mode.toString())) return;

            Object rawTiles = axial_cosmetics$getTiles();
            if (!(rawTiles instanceof List<?> tiles)) return;

            Object potionsHudTile = null;
            for (Object tile : tiles) {
                String label = axial_cosmetics$getTileLabel(tile);
                if ("MINIMAP".equals(label)) return;
                if ("POTIONS HUD".equals(label)) potionsHudTile = tile;
            }
            if (potionsHudTile == null) return;

            axial_cosmetics$addActionTile(
                    axial_cosmetics$getTileX(potionsHudTile) + 152,
                    axial_cosmetics$getTileY(potionsHudTile),
                    "MINIMAP",
                    () -> axial_cosmetics$openXaeroSettings((Screen) (Object) this)
            );
        } catch (ReflectiveOperationException | ClassCastException ignored) {
            // Leave the upstream menu unchanged if its private layout details change.
        }
    }

    private Object axial_cosmetics$getMode() throws ReflectiveOperationException {
        if (axial_cosmetics$modeField == null) {
            axial_cosmetics$modeField = this.getClass().getDeclaredField("mode");
            axial_cosmetics$modeField.setAccessible(true);
        }
        return axial_cosmetics$modeField.get(this);
    }

    private Object axial_cosmetics$getTiles() throws ReflectiveOperationException {
        if (axial_cosmetics$tilesField == null) {
            axial_cosmetics$tilesField = this.getClass().getDeclaredField("tiles");
            axial_cosmetics$tilesField.setAccessible(true);
        }
        return axial_cosmetics$tilesField.get(this);
    }

    private static String axial_cosmetics$getTileLabel(Object tile) throws ReflectiveOperationException {
        if (axial_cosmetics$tileLabelField == null) {
            axial_cosmetics$tileLabelField = tile.getClass().getDeclaredField("label");
            axial_cosmetics$tileLabelField.setAccessible(true);
        }
        return (String) axial_cosmetics$tileLabelField.get(tile);
    }

    private static int axial_cosmetics$getTileX(Object tile) throws ReflectiveOperationException {
        if (axial_cosmetics$tileXField == null) {
            axial_cosmetics$tileXField = tile.getClass().getDeclaredField("x");
            axial_cosmetics$tileXField.setAccessible(true);
        }
        return axial_cosmetics$tileXField.getInt(tile);
    }

    private static int axial_cosmetics$getTileY(Object tile) throws ReflectiveOperationException {
        if (axial_cosmetics$tileYField == null) {
            axial_cosmetics$tileYField = tile.getClass().getDeclaredField("y");
            axial_cosmetics$tileYField.setAccessible(true);
        }
        return axial_cosmetics$tileYField.getInt(tile);
    }

    private void axial_cosmetics$addActionTile(int x, int y, String label, Runnable action) throws ReflectiveOperationException {
        if (axial_cosmetics$addActionTileMethod == null) {
            Class<?> booleanSupplierClass = Class.forName("org.axial.axialutils.client.AxialConfigScreen$BooleanSupplier");
            axial_cosmetics$addActionTileMethod = this.getClass().getDeclaredMethod("addActionTile", int.class, int.class, String.class, Runnable.class, booleanSupplierClass);
            axial_cosmetics$addActionTileMethod.setAccessible(true);
        }
        axial_cosmetics$addActionTileMethod.invoke(this, x, y, label, action, null);
    }

    private static void axial_cosmetics$openXaeroSettings(Screen parent) {
        try {
            Class<?> screenClass = Class.forName("xaero.common.gui.GuiMinimapMain");
            Screen settingsScreen = (Screen) screenClass.getConstructor(Screen.class).newInstance(parent);
            MinecraftClient.getInstance().setScreen(settingsScreen);
        } catch (ReflectiveOperationException | ClassCastException ignored) {
            // Xaero's Minimap is not available or changed its settings screen API.
        }
    }
}
