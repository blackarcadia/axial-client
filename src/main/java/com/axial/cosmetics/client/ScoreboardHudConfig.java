package com.axial.cosmetics.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.nio.file.*;

/** Keep vanilla placement until the player explicitly moves the sidebar. */
public final class ScoreboardHudConfig {
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    private static Position position=new Position();
    private ScoreboardHudConfig(){}
    private static Path path(){return FabricLoader.getInstance().getConfigDir().resolve("axial-cosmetics-scoreboard.json");}
    public static void load(){load(path());}
    static void load(Path path){
        position=new Position();if(!Files.exists(path))return;
        try(var reader=Files.newBufferedReader(path)){
            Position loaded=GSON.fromJson(reader,Position.class);
            if(loaded!=null&&loaded.x>=0&&loaded.y>=0)position=loaded;
        }catch(IOException|JsonParseException ex){LoggerFactory.getLogger(ScoreboardHudConfig.class).warn("Could not load scoreboard position",ex);}
    }
    public static void save(){save(path());}
    static void save(Path path){
        try{Files.createDirectories(path.getParent());try(var writer=Files.newBufferedWriter(path)){GSON.toJson(position,writer);}}
        catch(IOException ex){LoggerFactory.getLogger(ScoreboardHudConfig.class).warn("Could not save scoreboard position",ex);}
    }
    public static void setPosition(int x,int y){position.x=Math.max(0,x);position.y=Math.max(0,y);}
    public static int x(int vanilla,int screenWidth,int width){return position.x<0?vanilla:clamp(position.x,screenWidth,width);}
    public static int y(int vanilla,int screenHeight,int height){return position.y<0?vanilla:clamp(position.y,screenHeight,height);}
    private static int clamp(int value,int screenSize,int size){return Math.clamp(value,0,Math.max(0,screenSize-size));}
    private static final class Position {int x=-1,y=-1;}
}
