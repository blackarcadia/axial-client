package com.axial.cosmetics.client;

import com.google.gson.*;
import java.io.IOException;
import java.nio.file.*;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.LoggerFactory;

/** An unset position retains the original centred XP animation. */
public final class IslandXpHudConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Position position = new Position();
    private IslandXpHudConfig() {}
    private static Path path() { return FabricLoader.getInstance().getConfigDir().resolve("axial-cosmetics-island-xp.json"); }
    public static void load() { load(path()); }
    static void load(Path path) {
        position = new Position();
        if (!Files.exists(path)) return;
        try (var reader = Files.newBufferedReader(path)) {
            Position value = GSON.fromJson(reader, Position.class);
            if (value != null && value.x >= 0 && value.y >= 0) position = value;
        } catch (IOException | JsonParseException ex) { LoggerFactory.getLogger(IslandXpHudConfig.class).warn("Could not load Island XP position", ex); }
    }
    public static void save() { save(path()); }
    static void save(Path path) {
        try {
            Files.createDirectories(path.getParent());
            try (var writer = Files.newBufferedWriter(path)) { GSON.toJson(position, writer); }
        } catch (IOException ex) { LoggerFactory.getLogger(IslandXpHudConfig.class).warn("Could not save Island XP position", ex); }
    }
    public static int x(int screen, int width) { return Math.clamp(position.x < 0 ? (screen - width) / 2 : position.x, 0, Math.max(0, screen - width)); }
    public static int y(int screen, int height) { return Math.clamp(position.y < 0 ? screen / 2 + 6 : position.y, 0, Math.max(0, screen - height)); }
    public static void setPosition(int x, int y) { position.x = Math.max(0, x); position.y = Math.max(0, y); }
    private static final class Position { int x = -1, y = -1; }
}
