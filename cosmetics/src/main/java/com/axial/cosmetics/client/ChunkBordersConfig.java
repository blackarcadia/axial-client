package com.axial.cosmetics.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ChunkBordersConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("axial-cosmetics-chunk-borders.json");
    private static Config config = new Config();
    private static Field renderChunkBordersField;
    private static Method switchRenderChunkborderMethod;

    private ChunkBordersConfig() {
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
    }

    public static boolean enabled() {
        return config.enabled;
    }

    public static void setEnabled(boolean enabled) {
        config.enabled = enabled;
        save();
    }

    public static void setEnabled(boolean enabled, MinecraftClient client) {
        setEnabled(enabled);
        apply(client);
    }

    public static void sync(MinecraftClient client) {
        apply(client);
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

    private static void apply(MinecraftClient client) {
        if (client == null || client.worldRenderer == null || client.worldRenderer.debugRenderer == null) {
            return;
        }

        try {
            boolean current = isEnabled(client);
            if (current != config.enabled) {
                switchRenderer(client);
            }
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static void switchRenderer(MinecraftClient client) throws ReflectiveOperationException {
        if (switchRenderChunkborderMethod == null) {
            switchRenderChunkborderMethod = client.worldRenderer.debugRenderer.getClass().getDeclaredMethod("switchRenderChunkborder");
            switchRenderChunkborderMethod.setAccessible(true);
        }
        switchRenderChunkborderMethod.invoke(client.worldRenderer.debugRenderer);
    }

    private static boolean isEnabled(MinecraftClient client) throws ReflectiveOperationException {
        if (renderChunkBordersField == null) {
            renderChunkBordersField = client.worldRenderer.debugRenderer.getClass().getDeclaredField("renderChunkborder");
            renderChunkBordersField.setAccessible(true);
        }
        return renderChunkBordersField.getBoolean(client.worldRenderer.debugRenderer);
    }

    private static final class Config {
        private boolean enabled = false;
    }
}
