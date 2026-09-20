package com.axial.cosmetics.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Persistent client-side waypoint storage. */
public final class WaypointConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("axial-cosmetics-waypoints.json");
    private static Data data = new Data();

    private WaypointConfig() { }

    public static void load() {
        if (!Files.exists(PATH)) {
            save();
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(PATH)) {
            Data loaded = GSON.fromJson(reader, Data.class);
            data = loaded == null ? new Data() : loaded;
            if (data.waypoints == null) data.waypoints = new ArrayList<>();
        } catch (IOException ignored) {
            data = new Data();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(PATH)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException ignored) { }
    }

    public static boolean enabled() { return data.enabled; }
    public static void setEnabled(boolean enabled) { data.enabled = enabled; save(); }
    public static List<Waypoint> waypoints() { return data.waypoints; }
    public static void add(Waypoint waypoint) { data.waypoints.add(waypoint); save(); }
    public static void remove(Waypoint waypoint) { data.waypoints.remove(waypoint); save(); }
    public static void changed() { save(); }

    private static final class Data {
        boolean enabled = true;
        List<Waypoint> waypoints = new ArrayList<>();
    }
}
