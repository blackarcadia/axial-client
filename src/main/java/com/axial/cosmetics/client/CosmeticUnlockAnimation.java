package com.axial.cosmetics.client;

/** Timings measured from a successful, verified redemption. */
final class CosmeticUnlockAnimation {
    static float ease(double value){float t=(float)Math.clamp(value,0,1);return 1-(1-t)*(1-t)*(1-t);}
    static float appear(double seconds){return ease(seconds/.35);}
    static float stamp(double seconds){return ease((seconds-.42)/.20);}
    static float stampScale(double seconds){
        if(seconds<.62)return 2.7f-1.7f*stamp(seconds);
        return 1+(float)(.12*Math.exp(-(seconds-.62)*14)*Math.sin((seconds-.62)*35));
    }
    static float shake(double seconds){return seconds<.62?0:(float)(Math.sin((seconds-.62)*65)*3*Math.exp(-(seconds-.62)*15));}
    static float fade(double seconds){return ease((seconds-2.25)/.55);}
    static boolean finished(double seconds){return seconds>=2.85;}
}
