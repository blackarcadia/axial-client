package com.axial.cosmetics.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Stores the local-only name-tag preference. Disabled preserves vanilla behaviour. */
public final class ThirdPersonNameTagsConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir()
            .resolve("axial-cosmetics-third-person-name-tags.json");
    private static Config config = new Config();

    private ThirdPersonNameTagsConfig() {}

    public static void load() {
        if (!Files.exists(PATH)) {
            save();
            return;
        }

        try (var reader = Files.newBufferedReader(PATH)) {
            Config loaded = GSON.fromJson(reader, Config.class);
            config = loaded == null ? new Config() : loaded;
        } catch (IOException | RuntimeException ignored) {
            config = new Config();
        }
    }

    public static boolean enabled() {
        return config.enabled;
    }

    public static void toggle() {
        config.enabled = !config.enabled;
        save();
    }

    private static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            try (var writer = Files.newBufferedWriter(PATH)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException ignored) {
        }
    }

    private static final class Config {
        private boolean enabled;
    }
}
