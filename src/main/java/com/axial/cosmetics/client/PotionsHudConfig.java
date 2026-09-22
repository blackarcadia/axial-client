package com.axial.cosmetics.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PotionsHudConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(PotionsHudConfig.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Config config = new Config();

    private PotionsHudConfig() { }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("axial-cosmetics-potions-hud.json");
    }

    public static void load() { load(configPath()); }

    static void load(Path path) {
        config = new Config();
        if (!Files.exists(path)) return;
        try (var reader = Files.newBufferedReader(path)) {
            Config loaded = GSON.fromJson(reader, Config.class);
            config = loaded == null ? new Config() : loaded;
            if (config.title == null) config.title = "Potions";
            config.title = config.title.substring(0, Math.min(48, config.title.length()));
            config.titleColor |= 0xFF000000;
        } catch (IOException | JsonParseException ex) {
            LOGGER.warn("Could not load Potions HUD settings", ex);
            config = new Config();
        }
    }

    public static void save() { save(configPath()); }

    static void save(Path path) {
        try {
            Files.createDirectories(path.getParent());
            try (var writer = Files.newBufferedWriter(path)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException ex) {
            LOGGER.warn("Could not save Potions HUD settings", ex);
        }
    }

    public static boolean isEnabled() { return config.enabled; }
    public static void toggle() { config.enabled = !config.enabled; save(); }
    public static boolean showBox() { return config.box; }
    public static void toggleBox() { config.box = !config.box; save(); }
    public static String title() { return config.title; }
    public static void setTitle(String title) { config.title = title; }
    public static int titleColor() { return config.titleColor; }
    public static void setTitleColor(int color) { config.titleColor = color | 0xFF000000; }
    public static int getX(int screenWidth, int width) { return clampPosition(config.x, screenWidth, width); }
    public static int getY(int screenHeight, int height) { return clampPosition(config.y, screenHeight, height); }
    static int clampPosition(int position, int screenSize, int size) {
        return Math.max(0, Math.min(position, Math.max(0, screenSize - size)));
    }
    public static void setPosition(int x, int y) { config.x = x; config.y = y; }

    private static final class Config {
        private boolean enabled;
        private boolean box = true;
        private String title = "Potions";
        private int titleColor = 0xFFFFFFFF;
        private int x = 8;
        private int y = 80;
    }
}
