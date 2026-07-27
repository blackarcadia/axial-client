package com.axial.cosmetics.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
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
    private static Field axial_cosmetics$tileXField;
    private static Field axial_cosmetics$tileYField;
    private static Field axial_cosmetics$tileWidthField;
    private static Field axial_cosmetics$tileHeightField;
    private static Field axial_cosmetics$tileKindField;
    private static Field axial_cosmetics$tileToggleGetterField;
    private static Field axial_cosmetics$tileToggleSetterField;
    private static Field axial_cosmetics$tileActionField;
    private static Field axial_cosmetics$tileAccentGetterField;
    private static Field axial_cosmetics$tileColorGetterField;
    private static Field axial_cosmetics$tileColorSetterField;
    private static Field axial_cosmetics$tileColorGroupField;
    private static Field axial_cosmetics$tileColorTargetField;
    private static Method axial_cosmetics$currentPanelBaseYMethod;
    private static Method axial_cosmetics$currentViewportHeightMethod;
    private static Method axial_cosmetics$getCurrentScrollOffsetMethod;
    private static Method axial_cosmetics$tileContainsMethod;
    private static Constructor<?> axial_cosmetics$menuTileConstructor;
    private static Constructor<?> axial_cosmetics$screenConstructor;
    private static Object axial_cosmetics$ruptureMode;

    @Inject(method = "rebuildLayout", at = @At("RETURN"), remap = false, order = 950)
    private void axial_cosmetics$replaceRuptureTileAction(CallbackInfo ci) {
        try {
            Object mode = axial_cosmetics$getMode();
            if (mode == null || !"MAIN".equals(mode.toString())) {
                return;
            }

            Object rawTiles = axial_cosmetics$getTiles();
            if (!(rawTiles instanceof List<?> tiles)) {
                return;
            }

            for (int i = 0; i < tiles.size(); i++) {
                Object tile = tiles.get(i);
                if (!RUPTURE_LABEL.equals(axial_cosmetics$getTileLabel(tile))) {
                    continue;
                }

                Object replacement = axial_cosmetics$newRuptureTile(tile);
                @SuppressWarnings("unchecked")
                List<Object> mutableTiles = (List<Object>) tiles;
                mutableTiles.set(i, replacement);
                return;
            }
        } catch (ReflectiveOperationException | ClassCastException ignored) {
            // Leave the upstream menu unchanged if axialutils internals change.
        }
    }

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

    private static int axial_cosmetics$getTileWidth(Object tile) throws ReflectiveOperationException {
        if (axial_cosmetics$tileWidthField == null) {
            axial_cosmetics$tileWidthField = tile.getClass().getDeclaredField("width");
            axial_cosmetics$tileWidthField.setAccessible(true);
        }
        return axial_cosmetics$tileWidthField.getInt(tile);
    }

    private static int axial_cosmetics$getTileHeight(Object tile) throws ReflectiveOperationException {
        if (axial_cosmetics$tileHeightField == null) {
            axial_cosmetics$tileHeightField = tile.getClass().getDeclaredField("height");
            axial_cosmetics$tileHeightField.setAccessible(true);
        }
        return axial_cosmetics$tileHeightField.getInt(tile);
    }

    private static Object axial_cosmetics$getTileKind(Object tile) throws ReflectiveOperationException {
        if (axial_cosmetics$tileKindField == null) {
            axial_cosmetics$tileKindField = tile.getClass().getDeclaredField("kind");
            axial_cosmetics$tileKindField.setAccessible(true);
        }
        return axial_cosmetics$tileKindField.get(tile);
    }

    private static Object axial_cosmetics$getTileToggleGetter(Object tile) throws ReflectiveOperationException {
        if (axial_cosmetics$tileToggleGetterField == null) {
            axial_cosmetics$tileToggleGetterField = tile.getClass().getDeclaredField("toggleGetter");
            axial_cosmetics$tileToggleGetterField.setAccessible(true);
        }
        return axial_cosmetics$tileToggleGetterField.get(tile);
    }

    private static Object axial_cosmetics$getTileToggleSetter(Object tile) throws ReflectiveOperationException {
        if (axial_cosmetics$tileToggleSetterField == null) {
            axial_cosmetics$tileToggleSetterField = tile.getClass().getDeclaredField("toggleSetter");
            axial_cosmetics$tileToggleSetterField.setAccessible(true);
        }
        return axial_cosmetics$tileToggleSetterField.get(tile);
    }

    private static Object axial_cosmetics$getTileAction(Object tile) throws ReflectiveOperationException {
        if (axial_cosmetics$tileActionField == null) {
            axial_cosmetics$tileActionField = tile.getClass().getDeclaredField("action");
            axial_cosmetics$tileActionField.setAccessible(true);
        }
        return axial_cosmetics$tileActionField.get(tile);
    }

    private static Object axial_cosmetics$getTileAccentGetter(Object tile) throws ReflectiveOperationException {
        if (axial_cosmetics$tileAccentGetterField == null) {
            axial_cosmetics$tileAccentGetterField = tile.getClass().getDeclaredField("accentGetter");
            axial_cosmetics$tileAccentGetterField.setAccessible(true);
        }
        return axial_cosmetics$tileAccentGetterField.get(tile);
    }

    private static Object axial_cosmetics$getTileColorGetter(Object tile) throws ReflectiveOperationException {
        if (axial_cosmetics$tileColorGetterField == null) {
            axial_cosmetics$tileColorGetterField = tile.getClass().getDeclaredField("colorGetter");
            axial_cosmetics$tileColorGetterField.setAccessible(true);
        }
        return axial_cosmetics$tileColorGetterField.get(tile);
    }

    private static Object axial_cosmetics$getTileColorSetter(Object tile) throws ReflectiveOperationException {
        if (axial_cosmetics$tileColorSetterField == null) {
            axial_cosmetics$tileColorSetterField = tile.getClass().getDeclaredField("colorSetter");
            axial_cosmetics$tileColorSetterField.setAccessible(true);
        }
        return axial_cosmetics$tileColorSetterField.get(tile);
    }

    private static Object axial_cosmetics$getTileColorGroup(Object tile) throws ReflectiveOperationException {
        if (axial_cosmetics$tileColorGroupField == null) {
            axial_cosmetics$tileColorGroupField = tile.getClass().getDeclaredField("colorGroup");
            axial_cosmetics$tileColorGroupField.setAccessible(true);
        }
        return axial_cosmetics$tileColorGroupField.get(tile);
    }

    private static Object axial_cosmetics$getTileColorTarget(Object tile) throws ReflectiveOperationException {
        if (axial_cosmetics$tileColorTargetField == null) {
            axial_cosmetics$tileColorTargetField = tile.getClass().getDeclaredField("colorTarget");
            axial_cosmetics$tileColorTargetField.setAccessible(true);
        }
        return axial_cosmetics$tileColorTargetField.get(tile);
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

    private Object axial_cosmetics$newRuptureTile(Object templateTile) throws ReflectiveOperationException {
        if (axial_cosmetics$menuTileConstructor == null) {
            Class<?> tileClass = templateTile.getClass();
            Class<?> outerClass = Class.forName("org.axial.axialutils.client.AxialConfigScreen");
            axial_cosmetics$menuTileConstructor = tileClass.getDeclaredConstructor(
                    outerClass,
                    int.class,
                    int.class,
                    int.class,
                    int.class,
                    String.class,
                    tileClass.getDeclaredField("kind").getType(),
                    tileClass.getDeclaredField("toggleGetter").getType(),
                    tileClass.getDeclaredField("toggleSetter").getType(),
                    Runnable.class,
                    tileClass.getDeclaredField("accentGetter").getType(),
                    tileClass.getDeclaredField("colorGetter").getType(),
                    tileClass.getDeclaredField("colorSetter").getType(),
                    tileClass.getDeclaredField("colorGroup").getType(),
                    tileClass.getDeclaredField("colorTarget").getType()
            );
            axial_cosmetics$menuTileConstructor.setAccessible(true);
        }

        int x = axial_cosmetics$getTileX(templateTile);
        int y = axial_cosmetics$getTileY(templateTile);
        int width = axial_cosmetics$getTileWidth(templateTile);
        int height = axial_cosmetics$getTileHeight(templateTile);
        Object kind = axial_cosmetics$getTileKind(templateTile);
        Object toggleGetter = axial_cosmetics$getTileToggleGetter(templateTile);
        Object toggleSetter = axial_cosmetics$getTileToggleSetter(templateTile);
        Object accentGetter = axial_cosmetics$getTileAccentGetter(templateTile);
        Object colorGetter = axial_cosmetics$getTileColorGetter(templateTile);
        Object colorSetter = axial_cosmetics$getTileColorSetter(templateTile);
        Object colorGroup = axial_cosmetics$getTileColorGroup(templateTile);
        Object colorTarget = axial_cosmetics$getTileColorTarget(templateTile);

        Runnable action = () -> {
            try {
                MinecraftClient.getInstance().setScreen(axial_cosmetics$newRuptureScreen());
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        };

        return axial_cosmetics$menuTileConstructor.newInstance(
                this,
                x,
                y,
                width,
                height,
                RUPTURE_LABEL,
                kind,
                toggleGetter,
                toggleSetter,
                action,
                accentGetter,
                colorGetter,
                colorSetter,
                colorGroup,
                colorTarget
        );
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
