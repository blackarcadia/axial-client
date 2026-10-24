package com.axial.cosmetics.client;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.nio.file.*;

public final class KeystrokesConfig {
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    private static Config config=new Config();
    private static int cachedMask=-1;
    private static boolean cachedStacked;
    private static KeystrokesLayout.Arrangement cachedLayout;

    private KeystrokesConfig(){}
    private static Path path(){return FabricLoader.getInstance().getConfigDir().resolve("axial-cosmetics-keystrokes.json");}
    public static void load(){load(path());}
    static void load(Path path){
        config=new Config();if(!Files.exists(path))return;
        try(var reader=Files.newBufferedReader(path)){var value=GSON.fromJson(reader,Config.class);if(value!=null)config=value;}
        catch(IOException|JsonParseException ex){org.slf4j.LoggerFactory.getLogger(KeystrokesConfig.class).warn("Could not load keystrokes settings",ex);}
        config.scale=Float.isFinite(config.scale)?Math.clamp(config.scale,.5f,3):1;
        config.opacity=Float.isFinite(config.opacity)?Math.clamp(config.opacity,.2f,1):.75f;
        config.textColor|=0xFF000000;config.pressedColor|=0xFF000000;
        config.visibleMask=visibleMask();
        int[] colors=new int[6];java.util.Arrays.fill(colors,config.textColor);
        if(config.keyColors!=null)for(int i=0;i<Math.min(6,config.keyColors.length);i++)colors[i]=config.keyColors[i]|0xFF000000;
        config.keyColors=colors;

    }
    public static void save(){save(path());}
    static void save(Path path){
        try{Files.createDirectories(path.getParent());try(var writer=Files.newBufferedWriter(path)){GSON.toJson(config,writer);}}
        catch(IOException ex){org.slf4j.LoggerFactory.getLogger(KeystrokesConfig.class).warn("Could not save keystrokes settings",ex);}
    }
    public static boolean enabled(){return config.enabled;}
    public static void toggle(){config.enabled=!config.enabled;save();}
    public static int visibleMask(){return (config.visibleMask==null?(config.mouse?63:15):config.visibleMask)&63;}
    public static boolean visible(int bit){checkBit(bit);return (visibleMask()&(1<<bit))!=0;}
    public static void setVisible(int bit,boolean value){checkBit(bit);config.visibleMask=value?visibleMask()|(1<<bit):visibleMask()&~(1<<bit);save();}
    public static boolean mouse(){return (visibleMask()&48)!=0;}
    public static void setMouse(boolean value){config.visibleMask=value?visibleMask()|48:visibleMask()&~48;save();}
    private static void checkBit(int bit){if(bit<0||bit>=6)throw new IllegalArgumentException("Unknown keystroke");}
    static KeystrokesLayout.Arrangement layout(){
        int mask=visibleMask();
        if(cachedLayout==null||cachedMask!=mask||cachedStacked!=config.stacked){cachedMask=mask;cachedStacked=config.stacked;cachedLayout=KeystrokesLayout.arrange(config.stacked,mask);}
        return cachedLayout;
    }
    public static boolean stacked(){return config.stacked;}
    public static void setStacked(boolean value){config.stacked=value;save();}
    public static float scale(){return config.scale;}
    public static void setScale(float value){config.scale=Float.isFinite(value)?Math.clamp(value,.5f,3):1;save();}
    public static float opacity(){return config.opacity;}
    public static void setOpacity(float value){config.opacity=Float.isFinite(value)?Math.clamp(value,.2f,1):.75f;save();}
    public static int textColor(){return config.textColor;}
    public static int pressedColor(){return config.pressedColor;}
    public static int textColor(int bit){checkBit(bit);return config.keyColors!=null&&bit<config.keyColors.length?config.keyColors[bit]:config.textColor;}
    public static void setTextColor(int bit,int value){
        checkBit(bit);
        if(config.keyColors==null){config.keyColors=new int[6];java.util.Arrays.fill(config.keyColors,config.textColor);}
        config.keyColors[bit]=value|0xFF000000;save();
    }
    public static void setTextColor(int value){config.textColor=value|0xFF000000;config.keyColors=new int[6];java.util.Arrays.fill(config.keyColors,config.textColor);save();}
    public static void setPressedColor(int value){config.pressedColor=value|0xFF000000;save();}
    public static int width(){return (int)Math.ceil(layout().width()*config.scale);}
    public static int height(){return (int)Math.ceil(layout().height()*config.scale);}
    public static int x(int screen){return Math.clamp(config.x<0?8:config.x,0,Math.max(0,screen-width()));}
    public static int y(int screen){return Math.clamp(config.y<0?screen-height()-38:config.y,0,Math.max(0,screen-height()));}
    public static void setPosition(int x,int y){config.x=Math.max(0,x);config.y=Math.max(0,y);}
    public static void reset(){config=new Config();save();}
    private static final class Config {
        boolean enabled=false,mouse=true,stacked=true;
        Integer visibleMask;
        int[] keyColors;
        float scale=1,opacity=.75f;
        int x=-1,y=-1,textColor=0xFFF0F2F3,pressedColor=0xFF8860B7;
    }
}
