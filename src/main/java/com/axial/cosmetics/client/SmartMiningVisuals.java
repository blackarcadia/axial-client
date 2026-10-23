package com.axial.cosmetics.client;

/** Applies the original 40% mining fade without discarding an entity's tint. */
public final class SmartMiningVisuals {
    private SmartMiningVisuals() { }
    public static int color(int original,boolean fade) {
        if(!fade)return original;
        int alpha=Math.round((original>>>24)*.4f);
        return (alpha<<24)|(original&0xFFFFFF);
    }
}
