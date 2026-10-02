package com.axial.cosmetics.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Persistent visibility settings for the additional Information HUD lines. */
public final class InformationHudExtrasConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(InformationHudExtrasConfig.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Config config = new Config();

    private InformationHudExtrasConfig() { }

    public static void load() {
        Path path = path();
        config = new Config();
        if (!Files.exists(path)) return;
        try (var reader = Files.newBufferedReader(path)) {
            Config loaded = GSON.fromJson(reader, Config.class);
            if (loaded != null) config = loaded;
        } catch (IOException | JsonParseException ex) {
            LOGGER.warn("Could not load Information HUD extra settings", ex);
        }
    }

    public static boolean showServer() { return config.server; }
    public static boolean showFacing() { return config.facing; }
    public static boolean showCoordinates() { return config.coordinates; }
    public static boolean showPing() { return config.ping; }
    public static void setServer(boolean value) { config.server = value; save(); }
    public static void setFacing(boolean value) { config.facing = value; save(); }
    public static void setCoordinates(boolean value) { config.coordinates = value; save(); }
    public static void setPing(boolean value) { config.ping = value; save(); }

    public static int enabledCount() {
        return (config.server ? 1 : 0) + (config.facing ? 1 : 0) + (config.coordinates ? 1 : 0) + (config.ping ? 1 : 0);
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("axial-cosmetics-information-hud.json");
    }

    private static void save() {
        try {
            Files.createDirectories(path().getParent());
            try (var writer = Files.newBufferedWriter(path())) {
                GSON.toJson(config, writer);
            }
        } catch (IOException ex) {
            LOGGER.warn("Could not save Information HUD extra settings", ex);
        }
    }

    private static final class Config {
        private boolean server;
        private boolean facing;
        private boolean coordinates;
        private boolean ping;
    }
}
