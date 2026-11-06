package com.axial.cosmetics.client;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.nio.file.Files;

public final class HitIndicatorsConfig {
    private static boolean enabled = true;
    private record Settings(boolean enabled) {}
    private HitIndicatorsConfig() {}
    private static java.nio.file.Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("axial-cosmetics-hit-indicators.json");
    }
    public static boolean enabled() { return enabled; }
    public static void load() {
        if (!Files.exists(path())) return;
        try (var reader = Files.newBufferedReader(path())) {
            var settings = new Gson().fromJson(reader, Settings.class);
            if (settings != null) enabled = settings.enabled();
        } catch (IOException | JsonParseException ex) {
            org.slf4j.LoggerFactory.getLogger(HitIndicatorsConfig.class).warn("Could not load hit indicators", ex);
        }
    }
    public static void setEnabled(boolean value) {
        enabled = value;
        if (!value) HitIndicators.clear();
        try {
            Files.createDirectories(path().getParent());
            Files.writeString(path(), new Gson().toJson(new Settings(value)));
        } catch (IOException ex) {
            org.slf4j.LoggerFactory.getLogger(HitIndicatorsConfig.class).warn("Could not save hit indicators", ex);
        }
    }
}
