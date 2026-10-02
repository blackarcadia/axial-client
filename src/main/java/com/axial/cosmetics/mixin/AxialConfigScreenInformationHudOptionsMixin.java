package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.InformationHudExtrasConfig;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

/** Adds the extra Information HUD toggles and removes obsolete BOX controls. */
@Mixin(targets = "org.axial.axialutils.client.AxialConfigScreen", remap = false, priority = 1500)
public abstract class AxialConfigScreenInformationHudOptionsMixin {
    @Inject(method = "rebuildLayout", at = @At("RETURN"), remap = false)
    private void axial_cosmetics$adjustHudOptions(CallbackInfo ci) {
        try {
            Object mode = field("mode").get(this);
            Object rawTiles = field("tiles").get(this);
            if (!(rawTiles instanceof List<?> rawList)) return;

            @SuppressWarnings("unchecked")
            List<Object> tiles = (List<Object>) rawList;
            tiles.removeIf(tile -> "BOX".equals(label(tile)));
            if (!"HUD".equals(String.valueOf(mode))) return;

            addToggle(tiles, "SERVER", InformationHudExtrasConfig::showServer, InformationHudExtrasConfig::setServer);
            addToggle(tiles, "FACING", InformationHudExtrasConfig::showFacing, InformationHudExtrasConfig::setFacing);
            addToggle(tiles, "COORDINATES", InformationHudExtrasConfig::showCoordinates, InformationHudExtrasConfig::setCoordinates);
            addToggle(tiles, "PING", InformationHudExtrasConfig::showPing, InformationHudExtrasConfig::setPing);
        } catch (ReflectiveOperationException ignored) {
            // Leave the native settings screen usable if its private API changes.
        }
    }

    private void addToggle(List<Object> tiles, String label, java.util.function.BooleanSupplier getter,
                           java.util.function.Consumer<Boolean> setter) throws ReflectiveOperationException {
        Class<?> screenType = this.getClass();
        Class<?> supplierType = Class.forName(screenType.getName() + "$BooleanSupplier");
        Class<?> consumerType = Class.forName(screenType.getName() + "$BooleanConsumer");
        Object supplier = Proxy.newProxyInstance(supplierType.getClassLoader(), new Class<?>[]{supplierType},
                (proxy, method, args) -> getter.getAsBoolean());
        Object consumer = Proxy.newProxyInstance(consumerType.getClassLoader(), new Class<?>[]{consumerType},
                (proxy, method, args) -> {
                    setter.accept((Boolean) args[0]);
                    return null;
                });
        Method add = screenType.getDeclaredMethod("addToggleTile", int.class, int.class, String.class, supplierType, consumerType);
        add.setAccessible(true);
        add.invoke(this, 0, 30, label, supplier, consumer);
        layoutNewTile(tiles.getLast(), tiles);
    }

    /**
     * This hook runs after the shared layout normalizer. Position the newly
     * appended tiles in the same three-column grid so they remain visible.
     */
    private void layoutNewTile(Object tile, List<Object> tiles) throws ReflectiveOperationException {
        int index = tiles.indexOf(tile);
        int panelX = (((Screen) (Object) this).width - 452) / 2;
        setInt(tile, "x", panelX + 18 + (index % 3) * 142);
        setInt(tile, "y", 30 + (index / 3) * 30);
        setInt(tile, "width", 132);
    }

    private static void setInt(Object target, String name, int value) throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.setInt(target, value);
    }

    private Field field(String name) throws NoSuchFieldException {
        Field field = this.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static String label(Object tile) {
        try {
            Field field = tile.getClass().getDeclaredField("label");
            field.setAccessible(true);
            return (String) field.get(tile);
        } catch (ReflectiveOperationException ignored) {
            return "";
        }
    }
}
