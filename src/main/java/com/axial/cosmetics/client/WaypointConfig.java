package com.axial.cosmetics.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.util.math.BlockPos;

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

    public static void create(String name, String world, String dimension, BlockPos position, int color) {
        Config config = load();
        config.waypoints.add(new Entry(name, world, dimension, position.getX(), position.getY(), position.getZ(), color));
        save(config);
    }

    public static List<Entry> waypoints() {
        return List.copyOf(load().waypoints);
    }

    public static List<Entry> waypointsFor(MinecraftClient client) {
        String world = worldId(client);
        return waypoints().stream().filter(entry -> world.equals(entry.world())).toList();
    }

    public static String worldId(MinecraftClient client) {
        ServerInfo server = client.getCurrentServerEntry();
        if (server != null) return "server:" + server.address.toLowerCase(java.util.Locale.ROOT);
        if (client.getServer() != null) return "singleplayer:" + client.getServer().getSaveProperties().getLevelName();
        return "unknown";
    }

    public static boolean enabled() { return load().enabled; }

    public static void setEnabled(boolean enabled) {
        Config config = load();
        config.enabled = enabled;
        save(config);
    }

    public static void rename(Entry old, String name) {
        Config config = load();
        int index = indexOf(config, old);
        if (index < 0 || index >= config.waypoints.size()) return;
        String updated = name.trim();
        if (updated.isEmpty() || old.name().equals(updated)) return;
        config.waypoints.set(index, new Entry(updated, old.world(), old.dimension(), old.x(), old.y(), old.z(), old.color()));
        save(config);
    }

    public static void delete(Entry removed) {
        Config config = load();
        int index = indexOf(config, removed);
        if (index < 0 || index >= config.waypoints.size()) return;
        config.waypoints.remove(index);
        save(config);
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

    private static int indexOf(Config config, Entry waypoint) {
        for (int index = 0; index < config.waypoints.size(); index++) {
            Entry entry = config.waypoints.get(index);
            if (entry.world().equals(waypoint.world()) && entry.dimension().equals(waypoint.dimension())
                    && entry.x() == waypoint.x() && entry.y() == waypoint.y() && entry.z() == waypoint.z()) return index;
        }
        return -1;
    }


    private static final class Config {
        private boolean enabled = true;
        private List<Entry> waypoints = new ArrayList<>();
    }

    public record Entry(String name, String world, String dimension, int x, int y, int z, int color) { }
}
