package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.PotionsHudSettingsScreen;
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
public abstract class AxialConfigScreenPotionsHudMixin {
    private static Field axial_cosmetics$potionsHudModeField;
    private static Field axial_cosmetics$potionsHudTilesField;
    private static Field axial_cosmetics$potionsHudTileLabelField;
    private static Field axial_cosmetics$potionsHudTileXField;
    private static Field axial_cosmetics$potionsHudTileYField;
    private static Method axial_cosmetics$potionsHudAddActionTileMethod;

    @Inject(method = "rebuildLayout", at = @At("RETURN"), remap = false, order = 970)
    private void axial_cosmetics$addPotionsHudButton(CallbackInfo ci) {
        try {
            Object mode = axial_cosmetics$potionsHudGetMode();
            if (mode == null || !"MAIN".equals(mode.toString())) {
                return;
            }

            Object rawTiles = axial_cosmetics$potionsHudGetTiles();
            if (!(rawTiles instanceof List<?> tiles)) {
                return;
            }

            Object titleOverlayTile = null;
            for (Object tile : tiles) {
                String label = axial_cosmetics$potionsHudGetTileLabel(tile);
                if ("POTIONS HUD".equals(label)) {
                    return;
                }
                if ("TITLE OVERLAY".equals(label)) {
                    titleOverlayTile = tile;
                }
            }

            if (titleOverlayTile == null) {
                return;
            }

            int x = axial_cosmetics$potionsHudGetTileX(titleOverlayTile);
            int y = axial_cosmetics$potionsHudGetTileY(titleOverlayTile) + 30;
            axial_cosmetics$potionsHudAddActionTile(x, y, "POTIONS HUD", () -> MinecraftClient.getInstance().setScreen(new PotionsHudSettingsScreen((Screen) (Object) this)));
        } catch (ReflectiveOperationException | ClassCastException ignored) {
            // Leave the upstream menu unchanged if its private layout details change.
        }
    }

    private Object axial_cosmetics$potionsHudGetMode() throws ReflectiveOperationException {
        if (axial_cosmetics$potionsHudModeField == null) {
            axial_cosmetics$potionsHudModeField = this.getClass().getDeclaredField("mode");
            axial_cosmetics$potionsHudModeField.setAccessible(true);
        }
        return axial_cosmetics$potionsHudModeField.get(this);
    }

    private Object axial_cosmetics$potionsHudGetTiles() throws ReflectiveOperationException {
        if (axial_cosmetics$potionsHudTilesField == null) {
            axial_cosmetics$potionsHudTilesField = this.getClass().getDeclaredField("tiles");
            axial_cosmetics$potionsHudTilesField.setAccessible(true);
        }
        return axial_cosmetics$potionsHudTilesField.get(this);
    }

    private static String axial_cosmetics$potionsHudGetTileLabel(Object tile) throws ReflectiveOperationException {
        if (axial_cosmetics$potionsHudTileLabelField == null) {
            axial_cosmetics$potionsHudTileLabelField = tile.getClass().getDeclaredField("label");
            axial_cosmetics$potionsHudTileLabelField.setAccessible(true);
        }
        return (String) axial_cosmetics$potionsHudTileLabelField.get(tile);
    }

    private static int axial_cosmetics$potionsHudGetTileX(Object tile) throws ReflectiveOperationException {
        if (axial_cosmetics$potionsHudTileXField == null) {
            axial_cosmetics$potionsHudTileXField = tile.getClass().getDeclaredField("x");
            axial_cosmetics$potionsHudTileXField.setAccessible(true);
        }
        return axial_cosmetics$potionsHudTileXField.getInt(tile);
    }

    private static int axial_cosmetics$potionsHudGetTileY(Object tile) throws ReflectiveOperationException {
        if (axial_cosmetics$potionsHudTileYField == null) {
            axial_cosmetics$potionsHudTileYField = tile.getClass().getDeclaredField("y");
            axial_cosmetics$potionsHudTileYField.setAccessible(true);
        }
        return axial_cosmetics$potionsHudTileYField.getInt(tile);
    }

    private void axial_cosmetics$potionsHudAddActionTile(int x, int y, String label, Runnable action) throws ReflectiveOperationException {
        if (axial_cosmetics$potionsHudAddActionTileMethod == null) {
            Class<?> booleanSupplierClass = Class.forName("org.axial.axialutils.client.AxialConfigScreen$BooleanSupplier");
            axial_cosmetics$potionsHudAddActionTileMethod = this.getClass().getDeclaredMethod("addActionTile", int.class, int.class, String.class, Runnable.class, booleanSupplierClass);
            axial_cosmetics$potionsHudAddActionTileMethod.setAccessible(true);
        }
        axial_cosmetics$potionsHudAddActionTileMethod.invoke(this, x, y, label, action, null);
    }
}
