package com.axial.cosmetics.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ItemScalerConfig {
    public static final float MIN_SCALE = 0.25f;
    public static final float MAX_SCALE = 3.0f;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("axial-cosmetics-item-scaler.json");
    private static Config config = new Config();

    private ItemScalerConfig() {
    }

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            save();
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(CONFIG_PATH)) {
            Config loaded = GSON.fromJson(reader, Config.class);
            config = loaded == null ? new Config() : loaded;
        } catch (IOException ignored) {
            config = new Config();
        }

        config.mainHandScale = clamp(config.mainHandScale);
        config.offHandScale = clamp(config.offHandScale);
    }

    public static float mainHandScale() {
        return clamp(config.mainHandScale);
    }

    public static void setMainHandScale(float scale) {
        config.mainHandScale = clamp(scale);
        save();
    }

    public static float offHandScale() {
        return clamp(config.offHandScale);
    }

    public static void setOffHandScale(float scale) {
        config.offHandScale = clamp(scale);
        save();
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException ignored) {
        }
    }

    private static float clamp(float value) {
        if (value <= 0.0f) {
            return 1.0f;
        }
        return Math.max(MIN_SCALE, Math.min(MAX_SCALE, value));
    }

    private static final class Config {
        private float mainHandScale = 1.0f;
        private float offHandScale = 1.0f;
    }
}
