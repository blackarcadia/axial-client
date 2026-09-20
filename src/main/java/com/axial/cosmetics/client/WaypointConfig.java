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
        save(config);

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

    public static boolean enabled() { return load().enabled; }

    public static void setEnabled(boolean enabled) {
        Config config = load();
        config.enabled = enabled;
        save(config);
    }

    public static void rename(int index, String name) {
        Config config = load();
        if (index < 0 || index >= config.waypoints.size()) return;
        Entry old = config.waypoints.get(index);
        String updated = name.trim();
        if (updated.isEmpty() || old.name().equals(updated)) return;
        config.waypoints.set(index, new Entry(updated, old.dimension(), old.x(), old.y(), old.z(), old.color()));
        save(config);
        for (AxialConfig.WarpWaypointEntry entry : AxialConfigManager.get().warpWaypoints) if (matches(entry, old)) entry.warpName = updated;
        AxialConfigManager.save();
    }

    public static void delete(int index) {
        Config config = load();
        if (index < 0 || index >= config.waypoints.size()) return;
        Entry removed = config.waypoints.remove(index);
        save(config);
        AxialConfigManager.get().warpWaypoints.removeIf(entry -> matches(entry, removed));
        AxialConfigManager.save();
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

    private static void save(Config config) {
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(config));
        } catch (IOException ignored) { }
    }

    private static boolean matches(AxialConfig.WarpWaypointEntry entry, Entry waypoint) {
        return waypoint.name().equals(entry.warpName) && waypoint.dimension().equals(entry.dimensionId)
                && waypoint.x() == entry.x && waypoint.y() == entry.y && waypoint.z() == entry.z;
    }

    private static final class Config {
        private boolean enabled = true;
        private List<Entry> waypoints = new ArrayList<>();
    }

    public record Entry(String name, String dimension, int x, int y, int z, int color) { }
}
