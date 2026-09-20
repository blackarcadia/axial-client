package com.axial.cosmetics.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.math.BlockPos;
import org.axial.axialutils.client.AxialConfig;
import org.axial.axialutils.client.AxialConfigManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class WaypointConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("axial-cosmetics-waypoints.json");
    private static Config cached;

    private WaypointConfig() { }

    public static void create(String name, String dimension, BlockPos position, int color) {
        Config config = load();
        config.waypoints.add(new Entry(name, dimension, position.getX(), position.getY(), position.getZ(), color));
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(config));
        } catch (IOException ignored) {
        }

        AxialConfig.WarpWaypointEntry entry = new AxialConfig.WarpWaypointEntry();
        entry.warpName = name;
        entry.dimensionId = dimension;
        entry.x = position.getX();
        entry.y = position.getY();
        entry.z = position.getZ();
        entry.enabled = true;
        AxialConfigManager.get().warpWaypoints.add(entry);
        AxialConfigManager.save();
    }

    public static List<Entry> waypoints() {
        return List.copyOf(load().waypoints);
    }

    private static Config load() {
        if (cached != null) return cached;
        if (!Files.exists(PATH)) return cached = new Config();
        try {
            Config config = GSON.fromJson(Files.readString(PATH), Config.class);
            if (config != null && config.waypoints != null) return cached = config;
        } catch (IOException ignored) {
        }
        return cached = new Config();
    }

    private static final class Config {
        private boolean enabled = true;
        private List<Entry> waypoints = new ArrayList<>();
    }

    public record Entry(String name, String dimension, int x, int y, int z, int color) { }
}
