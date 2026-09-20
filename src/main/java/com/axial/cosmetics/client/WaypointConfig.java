package com.axial.cosmetics.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.math.BlockPos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class WaypointConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("axial-cosmetics-waypoints.json");

    private WaypointConfig() { }

    public static void create(String name, String dimension, BlockPos position, int color) {
        Config config = load();
        config.waypoints.add(new Entry(name, dimension, position.getX(), position.getY(), position.getZ(), color));
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(config));
        } catch (IOException ignored) {
        }
    }

    private static Config load() {
        if (!Files.exists(PATH)) return new Config();
        try {
            Config config = GSON.fromJson(Files.readString(PATH), Config.class);
            if (config != null && config.waypoints != null) return config;
        } catch (IOException ignored) {
        }
        return new Config();
    }

    private static final class Config {
        private boolean enabled = true;
        private List<Entry> waypoints = new ArrayList<>();
    }

    private record Entry(String name, String dimension, int x, int y, int z, int color) { }
}
