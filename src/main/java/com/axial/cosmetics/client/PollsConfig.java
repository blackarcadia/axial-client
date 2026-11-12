package com.axial.cosmetics.client;

import com.google.gson.*;
import java.nio.file.*;
import java.io.IOException;
import net.fabricmc.loader.api.FabricLoader;

public final class PollsConfig {
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    private static Settings settings=new Settings();
    private PollsConfig() {}
    private static Path path(){return FabricLoader.getInstance().getConfigDir().resolve("axial-polls.json");}
    public static void load(){load(path());}
    static void load(Path path){
        settings=new Settings();
        if(!Files.exists(path))return;
        try(var reader=Files.newBufferedReader(path)){var parsed=GSON.fromJson(reader,Settings.class);if(parsed!=null)settings=parsed;}
        catch(IOException|JsonParseException ex){org.slf4j.LoggerFactory.getLogger(PollsConfig.class).warn("Could not load poll settings",ex);}
        settings.x=Math.max(-1,settings.x);settings.y=Math.max(-1,settings.y);
    }
    public static void save(){save(path());}
    static void save(Path path){
        try{Files.createDirectories(path.getParent());try(var writer=Files.newBufferedWriter(path)){GSON.toJson(settings,writer);}}
        catch(IOException ex){org.slf4j.LoggerFactory.getLogger(PollsConfig.class).warn("Could not save poll settings",ex);}
    }
    public static boolean enabled(){return settings.enabled;}
    public static void toggle(){settings.enabled=!settings.enabled;save();PollService.reset();}
    public static int x(int screen,int width){return Math.clamp(settings.x<0?screen-width-12:settings.x,0,Math.max(0,screen-width));}
    public static int y(int screen,int height){return Math.clamp(settings.y<0?48:settings.y,0,Math.max(0,screen-height));}
    public static void setPosition(int x,int y){settings.x=Math.max(0,x);settings.y=Math.max(0,y);}
    private static final class Settings {boolean enabled=true;int x=-1,y=-1;}
}
