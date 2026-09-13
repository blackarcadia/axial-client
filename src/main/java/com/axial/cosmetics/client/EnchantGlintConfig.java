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

public final class EnchantGlintConfig {
    public static final float MAX_STRENGTH = 4.0f;
    public static final int DEFAULT_COLOR = 0xFFA060FF;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Logger LOGGER = LoggerFactory.getLogger(EnchantGlintConfig.class);
    private static Config config = new Config();

    private EnchantGlintConfig() {}

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("axial-cosmetics-enchant-glint.json");
    }

    public static void load() {
        if (!Files.exists(path())) return;
        try (var reader = Files.newBufferedReader(path())) {
            Config loaded = GSON.fromJson(reader, Config.class);
            config = loaded == null ? new Config() : loaded;
            config.strength = clampStrength(config.strength);
            config.color |= 0xFF000000;
        } catch (IOException | JsonParseException ex) {
            config = new Config();
            LOGGER.warn("Could not load enchant glint settings", ex);
        }
    }

    public static float clampStrength(float value) {
        return Float.isFinite(value) ? Math.max(1.0f, Math.min(MAX_STRENGTH, value)) : 1.0f;
    }

    public static float strength() { return config.strength; }
    public static int color() { return config.color; }
    public static boolean customColor() { return config.customColor; }
    public static void setStrength(float value) { config.strength = clampStrength(value); save(); }
    public static void setColor(int value) {
        config.color = value | 0xFF000000;
        config.customColor = true;
    }
    public static void resetColor() {
        config.color = DEFAULT_COLOR;
        config.customColor = false;
        save();
    }

    public static void save() {
        try {
            Files.createDirectories(path().getParent());
            try (var writer = Files.newBufferedWriter(path())) {
                GSON.toJson(config, writer);
            }
        } catch (IOException ex) {
            LOGGER.warn("Could not save enchant glint settings", ex);
        }
    }

    private static final class Config {
        private float strength = 1.0f;
        private int color = DEFAULT_COLOR;
        private boolean customColor = false;
    }
}
