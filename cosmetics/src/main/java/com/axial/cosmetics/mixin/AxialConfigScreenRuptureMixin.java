package com.axial.cosmetics.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

@Mixin(targets = "org.axial.axialutils.client.AxialConfigScreen", remap = false)
public abstract class AxialConfigScreenRuptureMixin {
    private static final String RUPTURE_LABEL = "RUPTURE HIGHLIGHTS";
    private static Field axial_cosmetics$modeField;
    private static Field axial_cosmetics$tilesField;
    private static Field axial_cosmetics$tileLabelField;
    private static Method axial_cosmetics$currentPanelBaseYMethod;
    private static Method axial_cosmetics$currentViewportHeightMethod;
    private static Method axial_cosmetics$getCurrentScrollOffsetMethod;
    private static Method axial_cosmetics$tileContainsMethod;
    private static Constructor<?> axial_cosmetics$screenConstructor;
    private static Object axial_cosmetics$ruptureMode;

    @Inject(method = "method_25402", at = @At("RETURN"), cancellable = true, remap = false)
    private void axial_cosmetics$openRuptureHighlightsFallback(Click event, boolean focused, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            return;
        }

        try {
            Object mode = axial_cosmetics$getMode();
            if (mode == null || !"MAIN".equals(mode.toString())) {
                return;
            }

            Object rawTiles = axial_cosmetics$getTiles();
            if (!(rawTiles instanceof List<?> tiles)) {
                return;
            }

            int panelBaseY = axial_cosmetics$currentPanelBaseY();
            int scrollOffset = axial_cosmetics$getCurrentScrollOffset();
            int viewportHeight = axial_cosmetics$currentViewportHeight();
            double mouseX = event.x();
            double mouseY = event.y();

            for (Object tile : tiles) {
                if (!RUPTURE_LABEL.equals(axial_cosmetics$getTileLabel(tile))) {
                    continue;
                }
                if (!axial_cosmetics$tileContains(tile, mouseX, mouseY, panelBaseY, scrollOffset, viewportHeight)) {
                    continue;
                }

                MinecraftClient.getInstance().setScreen(axial_cosmetics$newRuptureScreen());
                cir.setReturnValue(true);
                return;
            }
        } catch (ReflectiveOperationException | ClassCastException ignored) {
            // Leave the upstream behavior alone if axialutils internals change.
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

    private int axial_cosmetics$currentPanelBaseY() throws ReflectiveOperationException {
        if (axial_cosmetics$currentPanelBaseYMethod == null) {
            axial_cosmetics$currentPanelBaseYMethod = this.getClass().getDeclaredMethod("currentPanelBaseY");
            axial_cosmetics$currentPanelBaseYMethod.setAccessible(true);
        }
        return (int) axial_cosmetics$currentPanelBaseYMethod.invoke(this);
    }

    private int axial_cosmetics$currentViewportHeight() throws ReflectiveOperationException {
        if (axial_cosmetics$currentViewportHeightMethod == null) {
            axial_cosmetics$currentViewportHeightMethod = this.getClass().getDeclaredMethod("currentViewportHeight");
            axial_cosmetics$currentViewportHeightMethod.setAccessible(true);
        }
        return (int) axial_cosmetics$currentViewportHeightMethod.invoke(this);
    }

    private int axial_cosmetics$getCurrentScrollOffset() throws ReflectiveOperationException {
        if (axial_cosmetics$getCurrentScrollOffsetMethod == null) {
            axial_cosmetics$getCurrentScrollOffsetMethod = this.getClass().getDeclaredMethod("getCurrentScrollOffset");
            axial_cosmetics$getCurrentScrollOffsetMethod.setAccessible(true);
        }
        return (int) axial_cosmetics$getCurrentScrollOffsetMethod.invoke(this);
    }

    private boolean axial_cosmetics$tileContains(Object tile, double mouseX, double mouseY, int panelBaseY, int scrollOffset, int viewportHeight) throws ReflectiveOperationException {
        if (axial_cosmetics$tileContainsMethod == null) {
            axial_cosmetics$tileContainsMethod = tile.getClass().getDeclaredMethod(
                    "contains",
                    double.class,
                    double.class,
                    int.class,
                    int.class,
                    int.class
            );
            axial_cosmetics$tileContainsMethod.setAccessible(true);
        }
        return (boolean) axial_cosmetics$tileContainsMethod.invoke(tile, mouseX, mouseY, panelBaseY, scrollOffset, viewportHeight);
    }

    private Screen axial_cosmetics$newRuptureScreen() throws ReflectiveOperationException {
        if (axial_cosmetics$screenConstructor == null || axial_cosmetics$ruptureMode == null) {
            Class<?> screenClass = Class.forName("org.axial.axialutils.client.AxialConfigScreen");
            Class<?> modeClass = Class.forName("org.axial.axialutils.client.AxialConfigScreen$Mode");
            axial_cosmetics$ruptureMode = Enum.valueOf((Class<Enum>) modeClass.asSubclass(Enum.class), "RUPTURE");
            axial_cosmetics$screenConstructor = screenClass.getDeclaredConstructor(Screen.class, modeClass);
            axial_cosmetics$screenConstructor.setAccessible(true);
        }
        return (Screen) axial_cosmetics$screenConstructor.newInstance((Screen) (Object) this, axial_cosmetics$ruptureMode);
    }
}
