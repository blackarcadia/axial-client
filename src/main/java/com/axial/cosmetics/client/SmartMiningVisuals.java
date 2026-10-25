package com.axial.cosmetics.client;

/** Keeps mining-obstructing players faintly visible without discarding their tint. */
public final class SmartMiningVisuals {
    private SmartMiningVisuals() { }
    public static int color(int original,boolean fade) {
        if(!fade)return original;
        int alpha=Math.round((original>>>24)*.1f);
        return (alpha<<24)|(original&0xFFFFFF);
    }
}
