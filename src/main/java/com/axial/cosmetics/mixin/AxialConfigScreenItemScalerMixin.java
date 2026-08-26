package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.ItemScalerSettingsScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

@Mixin(targets = "org.axial.axialutils.client.AxialConfigScreen", remap = false)
public abstract class AxialConfigScreenItemScalerMixin {
    private static Field axial_cosmetics$itemScalerModeField;
    private static Field axial_cosmetics$itemScalerTilesField;
    private static Field axial_cosmetics$itemScalerTileLabelField;
    private static Field axial_cosmetics$itemScalerTileXField;
    private static Field axial_cosmetics$itemScalerTileYField;
    private static Method axial_cosmetics$addActionTileMethod;

    @Inject(method = "rebuildLayout", at = @At("RETURN"), remap = false, order = 900)
    private void axial_cosmetics$addItemScalerButton(CallbackInfo ci) {
        try {
            Object mode = axial_cosmetics$getMode();
            if (mode == null || !"MAIN".equals(mode.toString())) {
                return;
            }

            Object rawTiles = axial_cosmetics$getTiles();
            if (!(rawTiles instanceof List<?> tiles)) {
                return;
            }

            Object titleOverlayTile = null;
            for (Object tile : tiles) {
                String label = axial_cosmetics$getTileLabel(tile);
                if ("ITEM SCALER".equals(label)) {
                    return;
                }
                if ("TITLE OVERLAY".equals(label)) {
                    titleOverlayTile = tile;
                }
            }

            if (titleOverlayTile == null) {
                return;
            }

            int x = axial_cosmetics$getTileX(titleOverlayTile);
            int y = axial_cosmetics$getTileY(titleOverlayTile) + 30;
            axial_cosmetics$addActionTile(x, y, "ITEM SCALER", () -> MinecraftClient.getInstance().setScreen(new ItemScalerSettingsScreen((Screen) (Object) this)));
        } catch (ReflectiveOperationException | ClassCastException ignored) {
            // Leave the upstream menu unchanged if its private layout details change.
        }
    }

    private Object axial_cosmetics$getMode() throws ReflectiveOperationException {
        if (axial_cosmetics$itemScalerModeField == null) {
            axial_cosmetics$itemScalerModeField = this.getClass().getDeclaredField("mode");
            axial_cosmetics$itemScalerModeField.setAccessible(true);
        }
        return axial_cosmetics$itemScalerModeField.get(this);
    }

    private Object axial_cosmetics$getTiles() throws ReflectiveOperationException {
        if (axial_cosmetics$itemScalerTilesField == null) {
            axial_cosmetics$itemScalerTilesField = this.getClass().getDeclaredField("tiles");
            axial_cosmetics$itemScalerTilesField.setAccessible(true);
        }
        return axial_cosmetics$itemScalerTilesField.get(this);
    }

    private static String axial_cosmetics$getTileLabel(Object tile) throws ReflectiveOperationException {
        if (axial_cosmetics$itemScalerTileLabelField == null) {
            axial_cosmetics$itemScalerTileLabelField = tile.getClass().getDeclaredField("label");
            axial_cosmetics$itemScalerTileLabelField.setAccessible(true);
        }
        return (String) axial_cosmetics$itemScalerTileLabelField.get(tile);
    }

    private static int axial_cosmetics$getTileX(Object tile) throws ReflectiveOperationException {
        if (axial_cosmetics$itemScalerTileXField == null) {
            axial_cosmetics$itemScalerTileXField = tile.getClass().getDeclaredField("x");
            axial_cosmetics$itemScalerTileXField.setAccessible(true);
        }
        return axial_cosmetics$itemScalerTileXField.getInt(tile);
    }

    private static int axial_cosmetics$getTileY(Object tile) throws ReflectiveOperationException {
        if (axial_cosmetics$itemScalerTileYField == null) {
            axial_cosmetics$itemScalerTileYField = tile.getClass().getDeclaredField("y");
            axial_cosmetics$itemScalerTileYField.setAccessible(true);
        }
        return axial_cosmetics$itemScalerTileYField.getInt(tile);
    }

    private void axial_cosmetics$addActionTile(int x, int y, String label, Runnable action) throws ReflectiveOperationException {
        if (axial_cosmetics$addActionTileMethod == null) {
            Class<?> booleanSupplierClass = Class.forName("org.axial.axialutils.client.AxialConfigScreen$BooleanSupplier");
            axial_cosmetics$addActionTileMethod = this.getClass().getDeclaredMethod("addActionTile", int.class, int.class, String.class, Runnable.class, booleanSupplierClass);
            axial_cosmetics$addActionTileMethod.setAccessible(true);
        }
        axial_cosmetics$addActionTileMethod.invoke(this, x, y, label, action, null);
    }
}
