package com.axial.cosmetics.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Persists the master Waypoints switch without changing saved waypoint data. */
public final class WaypointFeatureConfig {
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("axial-cosmetics-waypoints.json");
    private static boolean enabled = true;
    private static boolean loaded;

    private WaypointFeatureConfig() { }

    public static boolean isEnabled() {
        load();
        return enabled;
    }

    public static void setEnabled(boolean value) {
        load();
        enabled = value;
        save();
    }

    private static void load() {
        if (loaded) return;
        loaded = true;
        if (!Files.exists(PATH)) return;
        try {
            JsonObject config = JsonParser.parseString(Files.readString(PATH)).getAsJsonObject();
            if (config.has("enabled")) enabled = config.get("enabled").getAsBoolean();
        } catch (IOException | IllegalStateException ignored) {
            // Default to enabled if a previous config cannot be read.
        }
    }

    private static void save() {
        try {
            JsonObject config = Files.exists(PATH)
                    ? JsonParser.parseString(Files.readString(PATH)).getAsJsonObject()
                    : new JsonObject();
            config.addProperty("enabled", enabled);
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, config.toString(), StandardCharsets.UTF_8);
        } catch (IOException | IllegalStateException ignored) {
            // The current session keeps the chosen state if the config cannot be saved.
        }
    }
}
