package com.axial.cosmetics.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ChunkBordersConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("axial-cosmetics-chunk-borders.json");
    private static final String DEBUG_ENTRIES_CLASS = "net.minecraft.client.gui.components.debug.DebugScreenEntries";
    private static final String DEBUG_STATUS_CLASS = "net.minecraft.client.gui.components.debug.DebugScreenEntryStatus";
    private static final String DEBUG_ENTRY_LIST_CLASS = "net.minecraft.client.gui.components.debug.DebugScreenEntryList";
    private static Config config = new Config();
    private static Field minecraftDebugEntriesField;
    private static Field minecraftDebugRendererField;
    private static Method debugEntriesGetStatusMethod;
    private static Method debugEntriesSetStatusMethod;
    private static Method debugRendererRefreshMethod;
    private static Object chunkBordersId;
    private static Object enabledStatus;
    private static Object disabledStatus;

    private ChunkBordersConfig() {
    }

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            save();
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(CONFIG_PATH)) {
            Config loaded = GSON.fromJson(reader, Config.class);
            config = loaded == null ? new Config() : loaded;
        } catch (IOException ignored) {
            config = new Config();
        }
    }

    public static boolean enabled() {
        return config.enabled;
    }

    public static void setEnabled(boolean enabled) {
        config.enabled = enabled;
        save();
    }

    public static void setEnabled(boolean enabled, MinecraftClient client) {
        setEnabled(enabled);
        apply(client);
    }

    public static void sync(MinecraftClient client) {
        apply(client);
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException ignored) {
        }
    }

    private static void apply(MinecraftClient client) {
        if (client == null) {
            return;
        }

        try {
            Object debugEntries = getDebugEntries(client);
            Object debugRenderer = getDebugRenderer(client);
            if (debugEntries == null || debugRenderer == null) {
                return;
            }

            Object desired = config.enabled ? enabledStatus() : disabledStatus();
            Object current = getStatus(debugEntries);
            if (current != desired) {
                setStatus(debugEntries, desired);
            }
            refreshRendererList(debugRenderer);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static Object getDebugEntries(MinecraftClient client) throws ReflectiveOperationException {
        if (minecraftDebugEntriesField == null) {
            minecraftDebugEntriesField = MinecraftClient.class.getDeclaredField("debugEntries");
            minecraftDebugEntriesField.setAccessible(true);
        }
        return minecraftDebugEntriesField.get(client);
    }

    private static Object getDebugRenderer(MinecraftClient client) throws ReflectiveOperationException {
        if (minecraftDebugRendererField == null) {
            minecraftDebugRendererField = MinecraftClient.class.getDeclaredField("debugRenderer");
            minecraftDebugRendererField.setAccessible(true);
        }
        return minecraftDebugRendererField.get(client);
    }

    private static Object getStatus(Object debugEntries) throws ReflectiveOperationException {
        if (debugEntriesGetStatusMethod == null) {
            Class<?> listClass = Class.forName(DEBUG_ENTRY_LIST_CLASS);
            debugEntriesGetStatusMethod = listClass.getDeclaredMethod("getStatus", Class.forName("net.minecraft.resources.Identifier"));
            debugEntriesGetStatusMethod.setAccessible(true);
        }
        return debugEntriesGetStatusMethod.invoke(debugEntries, chunkBordersId());
    }

    private static void setStatus(Object debugEntries, Object status) throws ReflectiveOperationException {
        if (debugEntriesSetStatusMethod == null) {
            Class<?> listClass = Class.forName(DEBUG_ENTRY_LIST_CLASS);
            debugEntriesSetStatusMethod = listClass.getDeclaredMethod(
                    "setStatus",
                    Class.forName("net.minecraft.resources.Identifier"),
                    Class.forName(DEBUG_STATUS_CLASS)
            );
            debugEntriesSetStatusMethod.setAccessible(true);
        }
        debugEntriesSetStatusMethod.invoke(debugEntries, chunkBordersId(), status);
    }

    private static void refreshRendererList(Object debugRenderer) throws ReflectiveOperationException {
        if (debugRendererRefreshMethod == null) {
            debugRendererRefreshMethod = debugRenderer.getClass().getDeclaredMethod("refreshRendererList");
            debugRendererRefreshMethod.setAccessible(true);
        }
        debugRendererRefreshMethod.invoke(debugRenderer);
    }

    private static Object chunkBordersId() throws ReflectiveOperationException {
        if (chunkBordersId == null) {
            Class<?> entriesClass = Class.forName(DEBUG_ENTRIES_CLASS);
            Field field = entriesClass.getDeclaredField("CHUNK_BORDERS");
            field.setAccessible(true);
            chunkBordersId = field.get(null);
        }
        return chunkBordersId;
    }

    private static Object enabledStatus() throws ReflectiveOperationException {
        if (enabledStatus == null) {
            Class<?> statusClass = Class.forName(DEBUG_STATUS_CLASS);
            Field field = statusClass.getDeclaredField("ALWAYS_ON");
            field.setAccessible(true);
            enabledStatus = field.get(null);
        }
        return enabledStatus;
    }

    private static Object disabledStatus() throws ReflectiveOperationException {
        if (disabledStatus == null) {
            Class<?> statusClass = Class.forName(DEBUG_STATUS_CLASS);
            Field field = statusClass.getDeclaredField("NEVER");
            field.setAccessible(true);
            disabledStatus = field.get(null);
        }
        return disabledStatus;
    }

    private static final class Config {
        private boolean enabled = false;
    }
}
