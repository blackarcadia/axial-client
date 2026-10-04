package com.axial.cosmetics.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Persistent state for the HUD minimap. */
public final class MinimapConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("axial-cosmetics-minimap.json");
    private static Settings SETTINGS = loadSettings();

    private MinimapConfig() { }

    public static boolean isEnabled() { return SETTINGS.enabled; }
    public static void toggle() { SETTINGS.enabled = !SETTINGS.enabled; save(); }
    public static int getSize() {
        float size = size();
        return size == 1.0f ? 84 : size == 2.0f ? 144 : size == 3.0f ? 180 : 108;
    }
    public static String sizeLabel() { return String.format("%.1fX", size()); }
    public static void cycleSize() {
        float current = size();
        SETTINGS.size = current == 1.0f ? 1.5f : current == 1.5f ? 2.0f : current == 2.0f ? 3.0f : 1.0f;
        save();
    }
    public static boolean isCircular() { return "CIRCLE".equals(SETTINGS.style); }
    public static String styleLabel() { return isCircular() ? "CIRCLE" : "SQUARE"; }
    public static void cycleStyle() { SETTINGS.style = isCircular() ? "SQUARE" : "CIRCLE"; save(); }
    public static float zoom() { return SETTINGS.zoom == null || SETTINGS.zoom < 0.5f || SETTINGS.zoom > 2.0f ? 0.75f : SETTINGS.zoom; }
    public static String zoomLabel() { return String.format("%.2fX", zoom()); }
    public static void cycleZoom() {
        float value = zoom();
        SETTINGS.zoom = value == 0.75f ? 1.0f : value == 1.0f ? 1.5f : value == 1.5f ? 2.0f : 0.75f;
        save();
    }
    public static int getX(int screenWidth) { int size = getSize(); return clamp(SETTINGS.x == null ? screenWidth - size - 8 : SETTINGS.x, 0, screenWidth - size); }
    public static int getY(int screenHeight) { int size = getSize(); return clamp(SETTINGS.y == null ? 8 : SETTINGS.y, 0, screenHeight - size); }
    public static void setPosition(int x, int y) { SETTINGS.x = x; SETTINGS.y = y; }
    public static void save() {
        try { Files.createDirectories(PATH.getParent()); Files.writeString(PATH, GSON.toJson(SETTINGS)); }
        catch (IOException ignored) { }
    }

    private static Settings loadSettings() {
        if (!Files.exists(PATH)) return new Settings();
        try {
            Settings settings = GSON.fromJson(Files.readString(PATH), Settings.class);
            return settings == null ? new Settings() : settings;
        } catch (IOException ignored) { return new Settings(); }
    }
    private static float size() {
        if (SETTINGS.size == null || (SETTINGS.size != 1.0f && SETTINGS.size != 1.5f && SETTINGS.size != 2.0f && SETTINGS.size != 3.0f)) return 1.5f;
        return SETTINGS.size;
    }
    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(value, Math.max(min, max))); }
    private static final class Settings { boolean enabled; Integer x; Integer y; Float size; Float zoom; String style = "SQUARE"; }
}
