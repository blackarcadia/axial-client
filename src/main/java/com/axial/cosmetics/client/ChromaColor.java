package com.axial.cosmetics.client;

import java.awt.Color;

/** Stores Chroma alongside its fallback RGB in the otherwise opaque colour settings. */
public final class ChromaColor {
    public static final long PERIOD_MILLIS=6000;
    private static final int TAG=0xFE000000;
    private ChromaColor(){}
    public static boolean enabled(int color){return (color&0xFF000000)==TAG;}
    public static int solid(int color){return color|0xFF000000;}
    public static int withChroma(int color,boolean enabled){return (color&0xFFFFFF)|(enabled?TAG:0xFF000000);}
    /** Preserve the Chroma flag when normalizing colour-only settings to opaque. */
    public static int opaque(int color){return enabled(color)?color:solid(color);}
    public static int resolve(int color){return resolve(color,System.nanoTime()/1_000_000L);}
    public static int resolve(int color,long millis){
        if(!enabled(color))return color;
        return Color.HSBtoRGB(Math.floorMod(millis,PERIOD_MILLIS)/(float)PERIOD_MILLIS,.85f,1);
    }
}
