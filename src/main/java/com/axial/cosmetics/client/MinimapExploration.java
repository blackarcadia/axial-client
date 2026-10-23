package com.axial.cosmetics.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.util.HashMap;
import java.util.Map;

/** Stores discovered terrain colours and the latest death marker for each world. */
public final class MinimapExploration {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("axial-cosmetics-explored-map.json");
    private static boolean writable=true;
    private static Data data = load();
    private static boolean dirty;
    private static long lastSave;
    private static long revision;

    private MinimapExploration() { }

    public static void discover(MinecraftClient client, int x, int z, int color) {
        String world = worldKey(client);
        Map<String, Integer> pixels = data.terrain.computeIfAbsent(world, ignored -> new HashMap<>());
        if (!Integer.valueOf(color).equals(pixels.put(x + "," + z, color))) {dirty = true;revision++;}
    }

    public static long revision(){return revision;}
    static Map<String,Integer> terrainFor(MinecraftClient client){return data.terrain.computeIfAbsent(worldKey(client),ignored->new HashMap<>());}
    static void discover(Map<String,Integer> pixels,int x,int z,int color) {
        if(!Integer.valueOf(color).equals(pixels.put(x+","+z,color))){dirty=true;revision++;}
    }

    public static int colorAt(MinecraftClient client, int x, int z) {
        Map<String, Integer> pixels = data.terrain.get(worldKey(client));
        return pixels == null ? 0xFF171B25 : pixels.getOrDefault(x + "," + z, 0xFF171B25);
    }

    public static void markDeath(MinecraftClient client) {
        if (client.player == null) return;
        data.deaths.put(worldKey(client), new Death(client.player.getBlockX(), client.player.getBlockZ()));
        dirty = true;
    }

    public static Death deathFor(MinecraftClient client) { return data.deaths.get(worldKey(client)); }

    private static String worldKey(MinecraftClient client) {
        return WaypointConfig.worldId(client) + "|" + (client.world == null ? "unknown" : client.world.getRegistryKey().getValue());
    }

    public static void saveIfDue() {
        if(System.currentTimeMillis()-lastSave>=10_000)flush();
    }

    public static void flush() {
        if(!dirty||!writable)return;
        try {
            Files.createDirectories(PATH.getParent());
            Path temp=Files.createTempFile(PATH.getParent(),"exploration-",".tmp");
            try {
                Files.writeString(temp,GSON.toJson(data));
                try{Files.move(temp,PATH,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
                catch(AtomicMoveNotSupportedException ex){Files.move(temp,PATH,StandardCopyOption.REPLACE_EXISTING);}
            } finally {Files.deleteIfExists(temp);}
            dirty=false;lastSave=System.currentTimeMillis();
        } catch(IOException ex){
            lastSave=System.currentTimeMillis();
            org.slf4j.LoggerFactory.getLogger(MinimapExploration.class).warn("Could not save explored map",ex);
        }
    }

    private static Data load() {
        if (!Files.exists(PATH)) return new Data();
        try {
            Data value = GSON.fromJson(Files.readString(PATH), Data.class);
            if (value != null) {
                if (value.terrain == null) value.terrain = new HashMap<>();
                if (value.deaths == null) value.deaths = new HashMap<>();
                return value;
            }
        } catch (IOException|JsonParseException ex) {
            try{Files.copy(PATH,PATH.resolveSibling(PATH.getFileName()+".backup-"+System.currentTimeMillis()));}
            catch(IOException backup){writable=false;}
            org.slf4j.LoggerFactory.getLogger(MinimapExploration.class).warn("Could not read explored map; preserved original data",ex);
        }
        return new Data();
    }

    public record Death(int x, int z) { }
    private static final class Data { Map<String, Map<String, Integer>> terrain = new HashMap<>(); Map<String, Death> deaths = new HashMap<>(); }
}
