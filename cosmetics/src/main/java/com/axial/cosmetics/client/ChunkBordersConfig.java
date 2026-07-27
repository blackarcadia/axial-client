package com.axial.cosmetics.client;

import com.axial.cosmetics.mixin.DebugRendererAccessor;
import com.axial.cosmetics.mixin.WorldRendererDebugAccessor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.debug.DebugRenderer;
import net.minecraft.client.render.WorldRenderer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ChunkBordersConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("axial-cosmetics-chunk-borders.json");
    private static Config config = new Config();

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
        if (client == null) {
            return;
        }

        WorldRenderer worldRenderer = client.worldRenderer;
        if (worldRenderer == null) {
            return;
        }

        if (!(worldRenderer instanceof WorldRendererDebugAccessor worldRendererAccessor)) {
            return;
        }

        DebugRenderer debugRenderer = worldRendererAccessor.axial_cosmetics$getDebugRenderer();
        if (debugRenderer == null) {
            return;
        }

        if (!(debugRenderer instanceof DebugRendererAccessor rendererAccessor)) {
            return;
        }

        boolean desired = config.enabled;
        boolean current = rendererAccessor.axial_cosmetics$isRenderChunkborder();
        if (current != desired) {
            rendererAccessor.axial_cosmetics$switchRenderChunkborder();
        }
    }

    private static final class Config {
        private boolean enabled = false;
    }
}
