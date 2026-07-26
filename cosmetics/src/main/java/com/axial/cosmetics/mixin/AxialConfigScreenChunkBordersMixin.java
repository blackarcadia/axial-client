package com.axial.cosmetics.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

@Mixin(targets = "org.axial.axialutils.client.AxialConfigScreen", remap = false)
public abstract class AxialConfigScreenChunkBordersMixin {
    private static Field axial_cosmetics$modeField;
    private static Field axial_cosmetics$tilesField;
    private static Field axial_cosmetics$tileLabelField;
    private static Field axial_cosmetics$tileXField;
    private static Field axial_cosmetics$tileYField;
    private static Field axial_cosmetics$renderChunkborderField;
    private static Class<?> axial_cosmetics$booleanSupplierClass;
    private static Method axial_cosmetics$addActionTileMethod;
    private static Method axial_cosmetics$switchRenderChunkborderMethod;

    @Inject(method = "rebuildLayout", at = @At("RETURN"), remap = false, order = 920)
    private void axial_cosmetics$addChunkBordersButton(CallbackInfo ci) {
        try {
            Object mode = axial_cosmetics$getMode();
            if (mode == null || !"MAIN".equals(mode.toString())) {
                return;
            }

            Object rawTiles = axial_cosmetics$getTiles();
            if (!(rawTiles instanceof List<?> tiles)) {
                return;
            }

            Object scoreboardTile = null;
            Object worldBorderTile = null;
            for (Object tile : tiles) {
                String label = axial_cosmetics$getTileLabel(tile);
                if ("CHUNK BORDERS".equals(label)) {
                    return;
                }
                if ("SCOREBOARD".equals(label)) {
                    scoreboardTile = tile;
                } else if ("WORLD BORDER".equals(label)) {
                    worldBorderTile = tile;
                }
            }

            if (worldBorderTile != null) {
                int x = axial_cosmetics$getTileX(worldBorderTile) + 154;
                int y = axial_cosmetics$getTileY(worldBorderTile);
                axial_cosmetics$addActionTile(x, y, "CHUNK BORDERS", this::axial_cosmetics$toggleChunkBorders, axial_cosmetics$chunkBordersAccentGetter());
                return;
            }

            if (scoreboardTile != null) {
                int x = axial_cosmetics$getTileX(scoreboardTile) + 308;
                int y = axial_cosmetics$getTileY(scoreboardTile);
                axial_cosmetics$addActionTile(x, y, "CHUNK BORDERS", this::axial_cosmetics$toggleChunkBorders, axial_cosmetics$chunkBordersAccentGetter());
            }
        } catch (ReflectiveOperationException | ClassCastException ignored) {
            // Leave the upstream menu unchanged if axialutils internals change.
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

    private void axial_cosmetics$addActionTile(int x, int y, String label, Runnable action, Object accentGetter) throws ReflectiveOperationException {
        if (axial_cosmetics$addActionTileMethod == null) {
            if (axial_cosmetics$booleanSupplierClass == null) {
                axial_cosmetics$booleanSupplierClass = Class.forName("org.axial.axialutils.client.AxialConfigScreen$BooleanSupplier");
            }
            axial_cosmetics$addActionTileMethod = this.getClass().getDeclaredMethod(
                    "addActionTile",
                    int.class,
                    int.class,
                    String.class,
                    Runnable.class,
                    axial_cosmetics$booleanSupplierClass
            );
            axial_cosmetics$addActionTileMethod.setAccessible(true);
        }
        axial_cosmetics$addActionTileMethod.invoke(this, x, y, label, action, accentGetter);
    }

    private void axial_cosmetics$toggleChunkBorders() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.worldRenderer != null && client.worldRenderer.debugRenderer != null) {
            try {
                if (axial_cosmetics$switchRenderChunkborderMethod == null) {
                    axial_cosmetics$switchRenderChunkborderMethod = client.worldRenderer.debugRenderer.getClass().getDeclaredMethod("switchRenderChunkborder");
                    axial_cosmetics$switchRenderChunkborderMethod.setAccessible(true);
                }
                axial_cosmetics$switchRenderChunkborderMethod.invoke(client.worldRenderer.debugRenderer);
            } catch (ReflectiveOperationException ignored) {
                // Keep the button harmless if the debug renderer changes.
            }
        }
    }

    private boolean axial_cosmetics$isChunkBordersEnabled() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.worldRenderer == null || client.worldRenderer.debugRenderer == null) {
                return false;
            }

            if (axial_cosmetics$renderChunkborderField == null) {
                axial_cosmetics$renderChunkborderField = client.worldRenderer.debugRenderer.getClass().getDeclaredField("renderChunkborder");
                axial_cosmetics$renderChunkborderField.setAccessible(true);
            }
            return axial_cosmetics$renderChunkborderField.getBoolean(client.worldRenderer.debugRenderer);
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private Object axial_cosmetics$chunkBordersAccentGetter() throws ReflectiveOperationException {
        if (axial_cosmetics$booleanSupplierClass == null) {
            axial_cosmetics$booleanSupplierClass = Class.forName("org.axial.axialutils.client.AxialConfigScreen$BooleanSupplier");
        }

        return Proxy.newProxyInstance(
                axial_cosmetics$booleanSupplierClass.getClassLoader(),
                new Class<?>[] { axial_cosmetics$booleanSupplierClass },
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("getAsBoolean".equals(name) || "get".equals(name)) {
                        return axial_cosmetics$isChunkBordersEnabled();
                    }
                    if ("toString".equals(name)) {
                        return "ChunkBordersAccentGetter";
                    }
                    if ("hashCode".equals(name)) {
                        return System.identityHashCode(proxy);
                    }
                    if ("equals".equals(name)) {
                        return args != null && args.length > 0 && proxy == args[0];
                    }
                    return null;
                }
        );
    }
}
