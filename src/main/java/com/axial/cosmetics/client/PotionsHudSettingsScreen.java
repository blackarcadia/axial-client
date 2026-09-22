package com.axial.cosmetics.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.axial.axialutils.client.HudTitleRenamerScreen;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BooleanSupplier;

/** Supplies potion controls to the native HUD submenu, including its navigation and layout. */
public final class PotionsHudSettingsScreen {
    private static final Map<Screen, Boolean> SCREENS = new WeakHashMap<>();

    private PotionsHudSettingsScreen() { }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Screen create(Screen parent) {
        try {
            Class<?> type = Class.forName("org.axial.axialutils.client.AxialConfigScreen");
            Class<? extends Enum> mode = Class.forName(type.getName() + "$Mode").asSubclass(Enum.class);
            Screen screen = (Screen) type.getConstructor(Screen.class, mode)
                    .newInstance(parent, Enum.valueOf(mode, "HUD"));
            SCREENS.put(screen, false);
            return screen;
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Could not create the native Potions HUD submenu", ex);
        }
    }

    public static boolean isPotions(Screen screen) {
        return SCREENS.containsKey(screen);
    }

    public static boolean isConfigScreen(Screen screen) {
        return screen != null && screen.getClass().getName().equals("org.axial.axialutils.client.AxialConfigScreen");
    }

    public static void rebuild(Screen screen) {
        try {
            ((List<?>) field(screen, "tiles").get(screen)).clear();
            addAction(screen, PotionsHudConfig.isEnabled() ? "ENABLED" : "DISABLED", PotionsHudConfig::toggle, PotionsHudConfig::isEnabled);
            addAction(screen, "BOX", PotionsHudConfig::toggleBox, PotionsHudConfig::showBox);
            addAction(screen, "TITLE", () -> MinecraftClient.getInstance().setScreen(new HudTitleRenamerScreen(screen,
                    "POTIONS HUD", PotionsHudConfig.title(), "Potions", value -> {
                        PotionsHudConfig.setTitle(value);
                        PotionsHudConfig.save();
                    })), null);
            boolean expanded = SCREENS.get(screen);
            addAction(screen, expanded ? "HUD COLORS -" : "HUD COLORS +", () -> SCREENS.put(screen, !SCREENS.get(screen)), null);
            if (expanded) addTitleColor(screen);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Could not build the native Potions HUD controls", ex);
        }
    }

    private static void addAction(Screen screen, String label, Runnable action, BooleanSupplier accent) throws ReflectiveOperationException {
        Class<?> supplier = Class.forName(screen.getClass().getName() + "$BooleanSupplier");
        Method add = screen.getClass().getDeclaredMethod("addActionTile", int.class, int.class, String.class, Runnable.class, supplier);
        add.setAccessible(true);
        Object getter = accent == null ? null : Proxy.newProxyInstance(supplier.getClassLoader(), new Class<?>[]{supplier},
                (proxy, method, args) -> accent.getAsBoolean());
        // The shared layout normalizer places and sizes these native tiles.
        add.invoke(screen, 0, 30, label, action, getter);
    }

    private static void addTitleColor(Screen screen) throws ReflectiveOperationException {
        String prefix = screen.getClass().getName() + "$";
        Class<?> group = Class.forName(prefix + "ColorGroup");
        Class<?> target = Class.forName(prefix + "ColorTarget");
        Class<?> getterType = Class.forName(prefix + "ColorGetter");
        Class<?> setterType = Class.forName(prefix + "ColorSetter");
        Object getter = Proxy.newProxyInstance(getterType.getClassLoader(), new Class<?>[]{getterType},
                (proxy, method, args) -> PotionsHudConfig.titleColor());
        Object setter = Proxy.newProxyInstance(setterType.getClassLoader(), new Class<?>[]{setterType}, (proxy, method, args) -> {
            PotionsHudConfig.setTitleColor((Integer) args[0]);
            PotionsHudConfig.save();
            return null;
        });
        Method add = screen.getClass().getDeclaredMethod("addColorTile", int.class, int.class, int.class, String.class,
                group, target, getterType, setterType);
        add.setAccessible(true);
        // The existing color-picker redirect handles this tile; no inline editor is opened.
        add.invoke(screen, 0, 30, 203, "POTIONS TITLE", null, null, getter, setter);
    }

    private static Field field(Screen screen, String name) throws NoSuchFieldException {
        Field field = screen.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}
