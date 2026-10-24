package com.axial.cosmetics.client;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.nio.file.*;

public final class CrosshairConfigManager {
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    private static CrosshairConfig config=new CrosshairConfig();
    private CrosshairConfigManager(){}
    private static Path path(){return FabricLoader.getInstance().getConfigDir().resolve("axial-crosshair.json");}
    public static CrosshairConfig load(){return load(path());}
    static CrosshairConfig load(Path path){
        config=new CrosshairConfig();
        if(Files.exists(path))try(var reader=Files.newBufferedReader(path)){
            CrosshairConfig loaded=GSON.fromJson(reader,CrosshairConfig.class);if(loaded!=null)config=loaded;
        }catch(IOException|JsonParseException ex){org.slf4j.LoggerFactory.getLogger(CrosshairConfigManager.class).warn("Could not load crosshair settings",ex);}
        config.normalize();return config;
    }
    public static CrosshairConfig get(){return config;}
    public static void save(){save(path());}
    static void save(Path path){
        config.normalize();
        try{Files.createDirectories(path.getParent());try(var writer=Files.newBufferedWriter(path)){GSON.toJson(config,writer);}}
        catch(IOException ex){org.slf4j.LoggerFactory.getLogger(CrosshairConfigManager.class).warn("Could not save crosshair settings",ex);}
    }
}
