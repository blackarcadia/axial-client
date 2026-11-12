package com.axial.cosmetics.client;

/** Seconds after server acceptance: fold, seal, launch, then dismiss. */
final class FeedbackSendAnimation {
    static float ease(double value){float t=(float)Math.clamp(value,0,1);return t*t*(3-2*t);}
    static float fold(double seconds){return ease(seconds/.22);}
    static float appear(double seconds){return ease((seconds-.12)/.16);}
    static float seal(double seconds){return ease((seconds-.28)/.22);}
    static float flight(double seconds){float t=ease((seconds-.55)/.40);return t*t;}
    static float text(double seconds){return ease((seconds-.40)/.15)*(1-ease((seconds-1.65)/.20));}
    static boolean finished(double seconds){return seconds>=1.90;}
}
