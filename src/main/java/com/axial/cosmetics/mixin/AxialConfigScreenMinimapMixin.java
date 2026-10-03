package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.MinimapConfig;
import org.axial.axialutils.client.AxialConfigManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

/** Adds the one-click minimap visibility control beside Potions HUD. */
@Mixin(targets = "org.axial.axialutils.client.AxialConfigScreen", remap = false)
public abstract class AxialConfigScreenMinimapMixin {
    private static Field modeField, tilesField, labelField, xField, yField;
    private static Method addActionTile;

    @Inject(method = "rebuildLayout", at = @At("RETURN"), remap = false, order = 980)
    private void axial_cosmetics$addMinimapToggle(CallbackInfo ci) {
        try {
            Object mode = field(modeField, "mode");
            if (mode == null || !"MAIN".equals(mode.toString())) return;
            Object value = field(tilesField, "tiles");
            if (!(value instanceof List<?> tiles)) return;
            Object potions = null;
            for (Object tile : tiles) {
                String label = (String) tileField(tile, labelField, "label");
                if ("MINIMAP".equals(label)) return;
                if ("POTIONS HUD".equals(label)) potions = tile;
            }
            if (potions == null) return;
            int x = (int) tileField(potions, xField, "x") + 144;
            int y = (int) tileField(potions, yField, "y");
            Class<?> supplier = Class.forName("org.axial.axialutils.client.AxialConfigScreen$BooleanSupplier");
            if (addActionTile == null) {
                addActionTile = this.getClass().getDeclaredMethod("addActionTile", int.class, int.class, String.class, Runnable.class, supplier);
                addActionTile.setAccessible(true);
            }
            Object highlighted = Proxy.newProxyInstance(supplier.getClassLoader(), new Class<?>[]{supplier}, (proxy, method, args) -> "getAsBoolean".equals(method.getName()) && MinimapConfig.isEnabled());
            addActionTile.invoke(this, x, y, "MINIMAP", (Runnable) () -> { MinimapConfig.toggle(); AxialConfigManager.save(); }, highlighted);
        } catch (ReflectiveOperationException | ClassCastException ignored) {
            // The upstream menu is private; do not break it if its internals move.
        }
    }

    private Object field(Field cached, String name) throws ReflectiveOperationException {
        Field resolved = cached;
        if (resolved == null) { resolved = this.getClass().getDeclaredField(name); resolved.setAccessible(true); setField(name, resolved); }
        return resolved.get(this);
    }
    private static Object tileField(Object tile, Field cached, String name) throws ReflectiveOperationException {
        Field resolved = cached;
        if (resolved == null) { resolved = tile.getClass().getDeclaredField(name); resolved.setAccessible(true); setTileField(name, resolved); }
        return resolved.get(tile);
    }
    private static void setField(String name, Field value) { if ("mode".equals(name)) modeField = value; else tilesField = value; }
    private static void setTileField(String name, Field value) { if ("label".equals(name)) labelField = value; else if ("x".equals(name)) xField = value; else yField = value; }
}
