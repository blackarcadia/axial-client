package com.axial.cosmetics.client;

/** Continuous motion in bounded batches; sustained gains cannot keep one popup alive. */
final class IslandXpAnimation {
    private static final long HOLD_MILLIS=1100,MAX_HOLD_MILLIS=1400,GAP_MILLIS=180;
    record Frame(double amount,float alpha,float lift,float scale,boolean active) { }
    private double target,displayed,alpha,y=6,pulse,velocity,pending;
    private long startedAt,lastGain,lastFrame,readyAt;
    private boolean active;

    void show(double gain,long now) {
        if(!Double.isFinite(gain)||gain<=0)return;
        advance(now);
        if(!active) {
            if(now<readyAt){pending=add(pending,gain);return;}
            begin(add(pending,gain),now);return;
        }
        // A fading popup is finished. Its incoming gains belong to the next batch.
        if(now>=fadeAt()){pending=add(pending,gain);return;}
        target=add(target,gain);
        velocity=Math.min(2,velocity+1.1);
        lastGain=now;
    }
    Frame frame(long now) {
        advance(now);
        return new Frame(displayed,(float)alpha,(float)y,(float)(1-.07*(1-alpha)+pulse),active);
    }
    void clear() {resetPose();pending=0;readyAt=0;}
    private void resetPose() {active=false;target=displayed=alpha=pulse=velocity=0;y=6;}
    private void begin(double gain,long now) {
        resetPose();active=true;target=gain;velocity=1.1;pending=0;
        startedAt=lastGain=lastFrame=now;
    }
    private long fadeAt() {return Math.min(lastGain+HOLD_MILLIS,startedAt+MAX_HOLD_MILLIS);}
    private void advance(long now) {
        if(!active) {
            if(pending>0&&now>=readyAt)begin(pending,now);
            return;
        }
        if(now<lastFrame)return;
        long fadeAt=fadeAt();
        double seconds=(now-lastFrame)/1000.0;
        double visibleSeconds=Math.clamp((fadeAt-lastFrame)/1000.0,0,seconds);
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
        // Anchor the gap to the batch timeline, not the next rendered frame.
        // Coming back after a pause must not impose another full cooldown.
        if(now>fadeAt&&alpha<.004){resetPose();readyAt=fadeAt+700+GAP_MILLIS;}
    }
    private static double add(double total,double gain) {return Math.min(Double.MAX_VALUE,total+gain);}
    private static double approach(double from,double to,double speed,double seconds) {
        return to+(from-to)*Math.exp(-speed*seconds);
    }
}
