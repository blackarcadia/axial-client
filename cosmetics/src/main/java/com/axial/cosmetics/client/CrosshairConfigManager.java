package com.axial.cosmetics.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Environment(EnvType.CLIENT)
public final class CrosshairConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("axial-crosshair.json");
    private static final float SIZE_MIN = 0.1f;
    private static final float SIZE_MAX = 4.0f;
    private static final float LENGTH_MIN = 0.0f;
    private static final float LENGTH_MAX = 32.0f;
    private static final float WIDTH_MIN = 2.0f;
    private static final float WIDTH_MAX = 12.0f;
    private static final float GAP_MIN = 0.0f;
    private static final float GAP_MAX = 20.0f;
    private static CrosshairConfig config = new CrosshairConfig();

    private CrosshairConfigManager() {
    }

    public static CrosshairConfig load() {
        if (!Files.exists(CONFIG_PATH)) {
            save();
            return config;
        }

        try (BufferedReader reader = Files.newBufferedReader(CONFIG_PATH)) {
            CrosshairConfig loaded = GSON.fromJson(reader, CrosshairConfig.class);
            config = loaded == null ? new CrosshairConfig() : loaded;
        } catch (IOException ignored) {
            config = new CrosshairConfig();
        }

        normalize();
        return config;
    }

    public static CrosshairConfig get() {
        return config;
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

    private static void normalize() {
        if (config.style == null) {
            config.style = CrosshairConfig.CrosshairStyle.LUNAR;
        }

        config.size = clamp(config.size, SIZE_MIN, SIZE_MAX, 1.0f);
        config.length = snapEven(config.length, LENGTH_MIN, LENGTH_MAX, 4.0f);
        config.width = snapEven(config.width, WIDTH_MIN, WIDTH_MAX, 2.0f);
        config.gap = snapEven(config.gap, GAP_MIN, GAP_MAX, 2.0f);

        if (config.outlineColor == 0) {
            config.outlineColor = 0xFF000000;
        }

        if (config.color == 0) {
            config.color = 0xFFFFFFFF;
        }
    }

    private static float clamp(float value, float min, float max, float fallback) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            return fallback;
        }
        return Math.max(min, Math.min(max, value));
    }

    private static float snapEven(float value, float min, float max, float fallback) {
        float clamped = clamp(value, min, max, fallback);
        return Math.max(min, Math.min(max, Math.round(clamped / 2.0f) * 2.0f));
    }
}
