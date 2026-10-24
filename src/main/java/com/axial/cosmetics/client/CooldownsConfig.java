package com.axial.cosmetics.client;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.nio.file.*;

public final class CooldownsConfig {
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    private static Config config=new Config();
    private CooldownsConfig(){}
    private static Path path(){return FabricLoader.getInstance().getConfigDir().resolve("axial-cosmetics-cooldowns.json");}
    public static void load(){load(path());}
    static void load(Path path){
        config=new Config();if(!Files.exists(path))return;
        try(var reader=Files.newBufferedReader(path)){Config loaded=GSON.fromJson(reader,Config.class);if(loaded!=null)config=loaded;}
        catch(IOException|JsonParseException ex){LoggerFactory.getLogger(CooldownsConfig.class).warn("Could not load cooldown settings",ex);}
    }
    public static void save(){save(path());}
    static void save(Path path){
        try{Files.createDirectories(path.getParent());try(var writer=Files.newBufferedWriter(path)){GSON.toJson(config,writer);}}
        catch(IOException ex){LoggerFactory.getLogger(CooldownsConfig.class).warn("Could not save cooldown settings",ex);}
    }
    public static boolean enabled(){return config.enabled;}
    public static void toggle(){config.enabled=!config.enabled;save();}
    public static boolean petsVisible(){return config.enabled&&config.pets;}
    public static boolean combatVisible(){return config.enabled&&config.combat;}
    public static boolean pets(){return config.pets;}
    public static boolean combat(){return config.combat;}
    public static void setPets(boolean visible){config.pets=visible;save();}
    public static void setCombat(boolean visible){config.combat=visible;save();}
    public static void reset(){config=new Config();save();}
    public static void setPosition(int x,int y){config.x=Math.max(0,x);config.y=Math.max(0,y);}
    public static int x(int screen){return Math.clamp(config.x<0?screen-76:config.x,0,Math.max(0,screen-CombatCooldownHud.SIZE));}
    public static int y(int screen){return Math.clamp(config.y<0?screen-124:config.y,0,Math.max(0,screen-CombatCooldownHud.SIZE));}
    private static final class Config {boolean enabled=true,pets=true,combat=true;int x=-1,y=-1;}
}
