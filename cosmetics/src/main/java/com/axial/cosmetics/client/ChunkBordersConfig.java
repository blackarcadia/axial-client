package com.axial.cosmetics.client;

import com.axial.cosmetics.mixin.MinecraftClientDebugHudEntryListAccessor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.debug.DebugHudEntries;
import net.minecraft.client.gui.hud.debug.DebugHudEntryVisibility;
import net.minecraft.client.gui.hud.debug.DebugHudProfile;
import net.minecraft.util.Identifier;

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

        if (!(client instanceof MinecraftClientDebugHudEntryListAccessor clientAccessor)) {
            return;
        }

        DebugHudProfile debugHudEntryList = clientAccessor.axial_cosmetics$getDebugHudEntryList();
        if (debugHudEntryList == null) {
            return;
        }

        boolean desired = config.enabled;
        DebugHudEntryVisibility visibility = desired ? DebugHudEntryVisibility.ALWAYS_ON : DebugHudEntryVisibility.NEVER;
        if (debugHudEntryList.getVisibility(CHUNK_BORDERS) != visibility) {
            debugHudEntryList.setEntryVisibility(CHUNK_BORDERS, visibility);
        }
    }

    private static final Identifier CHUNK_BORDERS = DebugHudEntries.CHUNK_BORDERS;

    private static final class Config {
        private boolean enabled = false;
    }
}
