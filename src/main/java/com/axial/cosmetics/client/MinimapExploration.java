package com.axial.cosmetics.client;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonWriter;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/** Discovered colours stay in primitive chunk arrays; streaming saves run on one worker. */
public final class MinimapExploration {
    private static final Gson GSON=new Gson();
    private static final Path PATH=FabricLoader.getInstance().getConfigDir().resolve("axial-cosmetics-explored-map.json");
    private static boolean writable=true;
    private static final Data data=load();
    private static final AtomicLong revision=new AtomicLong();
    private static final ExecutorService WRITER=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"axial-map-save");t.setDaemon(true);return t;});
    private static volatile long savedRevision;
    private static long lastSave;
    private static Future<?> pending;
    private static MinimapTerrain arrivingTerrain=new MinimapTerrain();
    private static String boundWorldIdentity;

    private MinimapExploration() { }
    public static void discover(MinecraftClient client,int x,int z,int color){discover(terrainFor(client),x,z,color);}
    public static long revision(){return revision.get();}
    static void beginWorldChange(){arrivingTerrain=new MinimapTerrain();boundWorldIdentity=null;}
    static MinimapTerrain terrainFor(MinecraftClient client){
        // Paper may have several worlds with the same Minecraft dimension key.
        // Wait for the plugin UUID instead of reusing another world's explored pixels.
        if(MinimapServerOverview.awaitingPreparedWorld())return arrivingTerrain;
        String identity=MinimapServerOverview.fullWorldIdentity();
        String key=worldKey(client)+(identity==null?"":"|"+identity);
        if(identity!=null&&!identity.equals(boundWorldIdentity)) {
            // A new visit starts with fresh local observations over the complete server base.
            data.terrain.put(key,arrivingTerrain);boundWorldIdentity=identity;revision.incrementAndGet();
        }
        return data.terrain.computeIfAbsent(key,ignored->new MinimapTerrain());
    }
    static void discover(MinimapTerrain terrain,int x,int z,int color){if(terrain.put(x,z,color))revision.incrementAndGet();}
    public static int colorAt(MinecraftClient client,int x,int z) {
        MinimapTerrain terrain=terrainFor(client);
        int color=terrain==null?0:terrain.get(x,z);
        return color==0?0xFF171B25:color;
    }
    public static void markDeath(MinecraftClient client) {
        if(client.player==null)return;
        data.deaths.put(worldKey(client),new Death(client.player.getBlockX(),client.player.getBlockZ()));revision.incrementAndGet();
    }
    public static Death deathFor(MinecraftClient client){return data.deaths.get(worldKey(client));}
    private static String worldKey(MinecraftClient client){return WaypointConfig.worldId(client)+"|"+(client.world==null?"unknown":client.world.getRegistryKey().getValue());}
    public static void saveIfDue(){if(System.currentTimeMillis()-lastSave>=10_000)flush();}
    public static void flush() {
        if(!writable||savedRevision==revision.get()||WRITER.isShutdown()||pending!=null&&!pending.isDone())return;
        lastSave=System.currentTimeMillis();pending=WRITER.submit(MinimapExploration::write);
    }
    /** Shutdown may wait for disk; regular ticks and disconnects never do. */
    public static void shutdown() {
        if(WRITER.isShutdown())return;
        if(writable)WRITER.submit(MinimapExploration::write);
        WRITER.shutdown();
        try{WRITER.awaitTermination(5,TimeUnit.SECONDS);}catch(InterruptedException ex){Thread.currentThread().interrupt();}
    }
    private static void write() {
        long version=revision.get();
        if(version==savedRevision)return;
        try {
            Files.createDirectories(PATH.getParent());
            Path temp=Files.createTempFile(PATH.getParent(),"exploration-",".tmp");
            try {
                try(var writer=new JsonWriter(Files.newBufferedWriter(temp))) {
                    writer.beginObject().name("terrain").beginObject();
                    for(var world:data.terrain.entrySet()) {
                        writer.name(world.getKey());world.getValue().write(writer);
                    }
                    writer.endObject().name("deaths").beginObject();
                    for(var death:data.deaths.entrySet()) {
                        writer.name(death.getKey()).beginObject().name("x").value(death.getValue().x()).name("z").value(death.getValue().z()).endObject();
                    }
                    writer.endObject().endObject();
                }
                try{Files.move(temp,PATH,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
                catch(AtomicMoveNotSupportedException ex){Files.move(temp,PATH,StandardCopyOption.REPLACE_EXISTING);}
                // Changes made during this save remain dirty for the next save.
                savedRevision=version;
            } finally {Files.deleteIfExists(temp);}
        } catch(IOException|UncheckedIOException ex){org.slf4j.LoggerFactory.getLogger(MinimapExploration.class).warn("Could not save explored map",ex);}
    }
    private static Data load() {
        Data result=new Data();
        if(!Files.exists(PATH))return result;
        try(var reader=Files.newBufferedReader(PATH)) {
            SavedData value=GSON.fromJson(reader,SavedData.class);
            if(value!=null) {
                if(value.terrain!=null)for(var world:value.terrain.entrySet()) {
                    if(world.getValue()==null)continue;
                    var terrain=new MinimapTerrain();
                    for(var pixel:world.getValue().entrySet()) {
                        String key=pixel.getKey();int comma=key.indexOf(',');
                        if(comma<0||pixel.getValue()==null)continue;
                        try{terrain.put(Integer.parseInt(key.substring(0,comma)),Integer.parseInt(key.substring(comma+1)),pixel.getValue());}
                        catch(NumberFormatException ignored){ }
                    }
                    result.terrain.put(world.getKey(),terrain);
                }
                if(value.deaths!=null)value.deaths.forEach((key,death)->{if(death!=null)result.deaths.put(key,death);});
            }
        } catch(IOException|JsonParseException ex) {
            try{Files.copy(PATH,PATH.resolveSibling(PATH.getFileName()+".backup-"+System.currentTimeMillis()));}catch(IOException backup){writable=false;}
            org.slf4j.LoggerFactory.getLogger(MinimapExploration.class).warn("Could not read explored map; preserved original data",ex);
        }
        return result;
    }
    public record Death(int x,int z) { }
    private static final class Data {
        final Map<String,MinimapTerrain> terrain=new ConcurrentHashMap<>();
        final Map<String,Death> deaths=new ConcurrentHashMap<>();
    }
    private static final class SavedData {
        Map<String,Map<String,Integer>> terrain=new HashMap<>();Map<String,Death> deaths=new HashMap<>();
    }
}
