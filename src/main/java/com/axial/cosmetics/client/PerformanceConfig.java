package com.axial.cosmetics.client;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import java.io.IOException;
import java.nio.file.*;

/** Rendering preferences; volatile publication also covers chunk builder workers. */
public final class PerformanceConfig {
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    private static volatile Config config=new Config();
    private PerformanceConfig(){}
    private static Path path(){return FabricLoader.getInstance().getConfigDir().resolve("axial-cosmetics-performance.json");}
    public static void load(){load(path());}
    static void load(Path path){
        Config loaded=new Config();
        if(Files.exists(path))try(var reader=Files.newBufferedReader(path)){
            Config parsed=GSON.fromJson(reader,Config.class);if(parsed!=null)loaded=parsed;
        }catch(IOException|JsonParseException ex){org.slf4j.LoggerFactory.getLogger(PerformanceConfig.class).warn("Could not load performance settings",ex);}
        loaded.fpsColor|=0xFF000000;config=loaded;
    }
    private static void save(){save(path());}
    static void save(Path path){
        try{Files.createDirectories(path.getParent());try(var writer=Files.newBufferedWriter(path)){GSON.toJson(config,writer);}}
        catch(IOException ex){throw new IllegalStateException("Could not save performance settings",ex);}
    }
    public static boolean displayFps(){return config.displayFps;}
    public static boolean droppedItems(){return config.droppedItems;}
    public static boolean liquids(){return config.liquids;}
    public static boolean disableSpawnerMobs(){return config.disableSpawnerMobs;}
    public static boolean particles(){return config.particles;}
    public static boolean weather(){return config.weather;}
    public static int fpsColor(){return config.fpsColor;}
    public static void setDisplayFps(boolean value){config.displayFps=value;save();}
    public static void setDroppedItems(boolean value){config.droppedItems=value;save();}
    public static void setLiquids(boolean value){boolean changed=config.liquids!=value;config.liquids=value;save();if(changed)rebuildChunks();}
    public static void setDisableSpawnerMobs(boolean value){config.disableSpawnerMobs=value;save();}
    public static void setParticles(boolean value){config.particles=value;save();if(!value)MinecraftClient.getInstance().particleManager.clearParticles();}
    public static void setWeather(boolean value){config.weather=value;save();}
    public static void setFpsColor(int value){config.fpsColor=value|0xFF000000;save();}
    public static void reset(){boolean rebuild=!config.liquids;config=new Config();save();if(rebuild)rebuildChunks();}
    private static void rebuildChunks(){var client=MinecraftClient.getInstance();if(client.world!=null)client.worldRenderer.reload();}
    private static final class Config {
        volatile boolean displayFps=false,droppedItems=true,liquids=true,disableSpawnerMobs=false,particles=true,weather=true;
        int fpsColor=0xFFFFFFFF;
    }
}
