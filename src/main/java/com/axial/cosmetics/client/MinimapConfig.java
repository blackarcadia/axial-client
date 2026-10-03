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
    public static int getX(int screenWidth) { return clamp(SETTINGS.x == null ? screenWidth - MinimapRenderer.SIZE - 8 : SETTINGS.x, 0, screenWidth - MinimapRenderer.SIZE); }
    public static int getY(int screenHeight) { return clamp(SETTINGS.y == null ? 8 : SETTINGS.y, 0, screenHeight - MinimapRenderer.SIZE); }
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
    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(value, Math.max(min, max))); }
    private static final class Settings { boolean enabled; Integer x; Integer y; }
}
