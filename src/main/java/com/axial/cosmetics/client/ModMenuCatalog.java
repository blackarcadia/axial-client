package com.axial.cosmetics.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.axial.axialutils.client.AxialConfigManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;

/** Bridges the bundled menu and its injected entries without replacing their settings actions. */
final class ModMenuCatalog {
    private ModMenuCatalog() {}

    static boolean isMain(Screen screen) throws ReflectiveOperationException {
        return "MAIN".equals(field(screen, "mode").toString());
    }
    static boolean isOptions(Screen screen) throws ReflectiveOperationException {
        return "OPTIONS".equals(field(screen,"mode").toString());
    }

    static Screen parent(Screen screen) throws ReflectiveOperationException { return (Screen)field(screen,"parent"); }

    static Screen child(Screen backend,Screen parent,String name) {
        try {
            Class<?> modeClass=field(backend,"mode").getClass();
            Field value=modeClass.getDeclaredField(name);
            value.setAccessible(true);
            var constructor=backend.getClass().getDeclaredConstructor(Screen.class,modeClass);
            constructor.setAccessible(true);
            return (Screen)constructor.newInstance(parent,value.get(null));
        } catch(ReflectiveOperationException ex) { throw new IllegalStateException("Could not open "+name,ex); }
    }

    static List<ModMenuEntry> read(Screen backend) throws ReflectiveOperationException {
        List<ModMenuEntry> entries = new ArrayList<>();
        for (Object tile : (List<?>) field(backend, "tiles")) {
            String label = (String) field(tile, "label");
            Runnable action = (Runnable) field(tile, "action");
            if (action == null) continue; // Upstream placeholders are not usable mods.
            String title = title(label);
            String flag = switch (label) {
                case "HUD" -> "hudEnabled";
                case "SATCHEL HELPER" -> "showSatchelsHud";
                case "CPS" -> "showCps";
                case "ARMOR HUD" -> "showArmorHud";
                case "TITLE OVERLAY" -> "titleOverlayEnabled";
                default -> null;
            };
            BooleanSupplier enabled = null;
            Runnable toggle = null, configure = action;
            if (flag != null) {
                Field configFlag = AxialConfigManager.get().getClass().getField(flag);
                enabled = () -> readFlag(configFlag);
                toggle = () -> {
                    try { configFlag.set(AxialConfigManager.get(), !readFlag(configFlag)); }
                    catch (IllegalAccessException ex) { throw new IllegalStateException(ex); }
                    AxialConfigManager.save();
                };
            } else if (List.of("FULL BRIGHT", "SMART MINING", "CONTRABAND", "SCOREBOARD", "WORLD BORDER").contains(label)) {
                Object supplier = field(tile, "accentGetter");
                if (supplier != null) {
                    Method get = supplier.getClass().getDeclaredMethod("getAsBoolean");
                    get.setAccessible(true);
                    enabled = () -> invokeBoolean(get, supplier);
                    toggle = action; // Retain the original toggle's side effects and persistence.
                    configure = null;
                }
            } else {
                switch (label) {
                    case "CROSSHAIR" -> {
                        enabled = () -> CrosshairConfigManager.get().enabled;
                        toggle = () -> { CrosshairConfigManager.get().enabled = !CrosshairConfigManager.get().enabled; CrosshairConfigManager.save(); };
                    }
                    case "CHUNK BORDERS" -> {
                        enabled = ChunkBordersConfig::enabled;
                        toggle = () -> ChunkBordersConfig.setEnabled(!ChunkBordersConfig.enabled(), MinecraftClient.getInstance());
                    }
                    case "COMPASS" -> { enabled = CompassConfig::isEnabled; toggle = CompassConfig::toggle; }
                    case "MINIMAP" -> { enabled = MinimapConfig::isEnabled; toggle = MinimapConfig::toggle; }
                    case "POTIONS HUD" -> { enabled = PotionsHudConfig::isEnabled; toggle = PotionsHudConfig::toggle; }
                    default -> { /* Configuration tools have a gear, without a fictional on/off state. */ }
                }
            }
            entries.add(new ModMenuEntry(title, description(label), category(label), icon(label), enabled, toggle, configure));
        }
        entries.add(new ModMenuEntry("Enchantments", "Server enchantment guide", ModMenuEntry.Category.MINING,
                "enchanted_book", null, null, () -> {
                    var client=MinecraftClient.getInstance();client.setScreen(new EnchantmentsScreen(client.currentScreen));
                }));
        return List.copyOf(entries);
    }

    static void openMode(Screen backend, String name) {
        try {
            Class<?> modeClass = field(backend, "mode").getClass();
            Field value = modeClass.getDeclaredField(name);
            value.setAccessible(true);
            Object mode = value.get(null);
            Method open = backend.getClass().getDeclaredMethod("openMode", modeClass);
            open.setAccessible(true);
            open.invoke(backend, mode);
        } catch (ReflectiveOperationException ex) { throw new IllegalStateException("Could not open " + name, ex); }
    }

    private static Object field(Object target, String name) throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static boolean readFlag(Field field) {
        try { return Boolean.TRUE.equals(field.get(AxialConfigManager.get())); }
        catch (IllegalAccessException ex) { throw new IllegalStateException(ex); }
    }

    private static boolean invokeBoolean(Method method, Object target) {
        try { return Boolean.TRUE.equals(method.invoke(target)); }
        catch (ReflectiveOperationException ex) { throw new IllegalStateException(ex); }
    }

    private static String title(String label) {
        return switch (label) {
            case "HUD", "CPS", "ARMOR HUD", "POTIONS HUD" -> label.equals("HUD") || label.equals("CPS") ? label
                    : label.equals("ARMOR HUD") ? "Armor HUD" : "Potions HUD";
            default -> {
                var words = label.toLowerCase(Locale.ROOT).split(" ");
                for (int i = 0; i < words.length; i++) if (!words[i].isEmpty()) words[i] = Character.toUpperCase(words[i].charAt(0)) + words[i].substring(1);
                yield String.join(" ", words);
            }
        };
    }

    private static ModMenuEntry.Category category(String label) {
        return switch (label) {
            case "HUD", "SATCHEL HELPER", "CPS", "ARMOR HUD", "POTIONS HUD", "SCOREBOARD", "COMPASS" -> ModMenuEntry.Category.HUD;
            case "SMART MINING", "CONTRABAND" -> ModMenuEntry.Category.MINING;
            case "FULL BRIGHT", "TITLE OVERLAY", "ITEM SCALER", "CROSSHAIR" -> ModMenuEntry.Category.VISUALS;
            default -> ModMenuEntry.Category.WORLD;
        };
    }

    private static String icon(String label) {
        return switch (label) {
            case "HUD" -> "monitor"; case "SATCHEL HELPER" -> "backpack";
            case "FULL BRIGHT" -> "sun"; case "SMART MINING" -> "pickaxe";
            case "CPS" -> "mouse"; case "ARMOR HUD" -> "shirt";
            case "CONTRABAND" -> "cube"; case "SCOREBOARD" -> "chart";
            case "CROSSHAIR" -> "crosshair"; case "COMPASS" -> "compass";
            case "POTIONS HUD" -> "potion"; case "MINIMAP", "WAYPOINTS" -> "map";
            case "ITEM SCALER" -> "scale"; case "TITLE OVERLAY" -> "monitor";
            default -> "globe";
        };
    }

    private static String description(String label) {
        return switch (label) {
            case "HUD" -> "On-screen information"; case "SATCHEL HELPER" -> "Ore satchel information";
            case "FULL BRIGHT" -> "Brighten dark areas"; case "SMART MINING" -> "Mining assistance";
            case "CPS" -> "Clicks per second"; case "ARMOR HUD" -> "Armor and durability";
            case "CONTRABAND" -> "Contraband highlights"; case "SCOREBOARD" -> "Scoreboard display";
            case "WORLD BORDER" -> "World boundary display"; case "CHUNK BORDERS" -> "Show chunk boundaries";
            case "CROSSHAIR" -> "Aim style and appearance"; case "ITEM SCALER" -> "Held item scale";
            case "COMPASS" -> "Direction at a glance"; case "MINIMAP" -> "Your surroundings";
            case "WAYPOINTS" -> "Saved world locations"; case "POTIONS HUD" -> "Active potion effects";
            case "TITLE OVERLAY" -> "Overlay appearance"; default -> "Open mod settings";
        };
    }
}
