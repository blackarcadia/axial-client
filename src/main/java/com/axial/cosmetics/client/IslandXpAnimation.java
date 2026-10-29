package com.axial.cosmetics.client;

/** Continuous, elapsed-time motion. New gains retarget the popup without resetting its pose. */
final class IslandXpAnimation {
    private static final double HOLD_MILLIS=1100;
    record Frame(double amount,float alpha,float lift,float scale,boolean active) { }
    private double target,displayed,alpha,y=6,pulse,velocity;
    private long lastGain,lastFrame;
    private boolean active;

    void show(double gain,long now) {
        if(!Double.isFinite(gain)||gain<=0)return;
        advance(now);
        if(!active) {
            target=displayed=alpha=pulse=velocity=0;y=6;
            active=true;lastFrame=now;
        }
        // A notification represents one uninterrupted burst of XP gains.
        target+=gain;
        if(!Double.isFinite(target))target=Double.MAX_VALUE;
        velocity=Math.min(2,velocity+1.1);
        lastGain=now;
    }
    Frame frame(long now) {
        advance(now);
        return new Frame(displayed,(float)alpha,(float)y,(float)(1-.07*(1-alpha)+pulse),active);
    }
    void clear() {active=false;target=displayed=alpha=pulse=velocity=0;y=6;}
    private void advance(long now) {
        if(!active)return;
        if(now<lastFrame)return;
        double seconds=(now-lastFrame)/1000.0;
        double visibleSeconds=Math.clamp((lastGain+HOLD_MILLIS-lastFrame)/1000.0,0,seconds);
        alpha=approach(alpha,1,12,visibleSeconds);
        y=approach(y,0,11,visibleSeconds);
        double fadingSeconds=seconds-visibleSeconds;
        alpha=approach(alpha,0,9,fadingSeconds);
        y=approach(y,-7,7,fadingSeconds);
        displayed=approach(displayed,target,13,seconds);
        if(Math.abs(target-displayed)<.01)displayed=target;
        double omega=16,term=velocity+omega*pulse,decay=Math.exp(-omega*seconds);
        pulse=(pulse+term*seconds)*decay;
        velocity=(velocity-omega*term*seconds)*decay;
        lastFrame=now;
        if(now-lastGain>HOLD_MILLIS&&alpha<.004)clear();
    }
    private static double approach(double from,double to,double speed,double seconds) {
        return to+(from-to)*Math.exp(-speed*seconds);
    }
}
