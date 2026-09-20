package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.WaypointSettingsScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/** Adds the Waypoints tile immediately beneath the existing Compass tile. */
@Mixin(targets = "org.axial.axialutils.client.AxialConfigScreen", remap = false)
public abstract class AxialConfigScreenWaypointsMixin {
    private static Field modeField, tilesField, labelField, xField, yField;
    private static Method addActionTile;

    @Inject(method = "rebuildLayout", at = @At("RETURN"), remap = false, order = 940)
    private void addWaypointsTile(CallbackInfo ci) {
        try {
            if (!"MAIN".equals(String.valueOf(field("mode").get(this)))) return;
            Object raw = field("tiles").get(this);
            if (!(raw instanceof List<?> tiles)) return;
            Object compass = null;
            for (Object tile : tiles) {
                String label = (String) tileField("label", tile).get(tile);
                if ("WAYPOINTS".equals(label)) return;
                if ("COMPASS".equals(label)) compass = tile;
            }
            if (compass == null) return;
            int x = tileField("x", compass).getInt(compass);
            int y = tileField("y", compass).getInt(compass) + 30;
            if (addActionTile == null) {
                Class<?> supplier = Class.forName("org.axial.axialutils.client.AxialConfigScreen$BooleanSupplier");
                addActionTile = getClass().getDeclaredMethod("addActionTile", int.class, int.class, String.class, Runnable.class, supplier);
                addActionTile.setAccessible(true);
            }
            addActionTile.invoke(this, x, y, "WAYPOINTS", (Runnable) () -> MinecraftClient.getInstance().setScreen(new WaypointSettingsScreen((Screen) (Object) this)), null);
        } catch (ReflectiveOperationException | ClassCastException ignored) { }
    }
    private Field field(String name) throws NoSuchFieldException { if ("mode".equals(name)) { if (modeField == null) modeField = accessible(getClass().getDeclaredField(name)); return modeField; } if (tilesField == null) tilesField = accessible(getClass().getDeclaredField(name)); return tilesField; }
    private static Field tileField(String name, Object tile) throws NoSuchFieldException { Field result; if ("label".equals(name)) { if (labelField == null) labelField = accessible(tile.getClass().getDeclaredField(name)); result = labelField; } else if ("x".equals(name)) { if (xField == null) xField = accessible(tile.getClass().getDeclaredField(name)); result = xField; } else { if (yField == null) yField = accessible(tile.getClass().getDeclaredField(name)); result = yField; } return result; }
    private static Field accessible(Field field) { field.setAccessible(true); return field; }
}
