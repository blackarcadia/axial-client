package com.axial.cosmetics.client;

/** Seconds after server acceptance: fold, seal, launch, then dismiss. */
final class FeedbackSendAnimation {
    static float ease(double value){float t=(float)Math.clamp(value,0,1);return t*t*(3-2*t);}
    static float fold(double seconds){return ease(seconds/.38);}
    static float appear(double seconds){return ease((seconds-.24)/.26);}
    static float seal(double seconds){return ease((seconds-.62)/.40);}
    static float flight(double seconds){float t=ease((seconds-1.18)/.80);return t*t;}
    static float text(double seconds){return ease((seconds-.95)/.25)*(1-ease((seconds-2.2)/.3));}
    static boolean finished(double seconds){return seconds>=2.55;}
}
