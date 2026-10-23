package com.axial.cosmetics.client;

import java.util.function.LongSupplier;

/** Monotonic, frame-rate-independent bulletin timing, with explicit hover pause. */
final class BulletinCarousel {
    static final long HOLD_NANOS=6_000_000_000L, FADE_NANOS=650_000_000L;
    private final int count;
    private final LongSupplier clock;
    private long last,elapsed;
    private int current,target;
    private boolean fading;

    BulletinCarousel(int count){this(count,System::nanoTime);}
    BulletinCarousel(int count,LongSupplier clock) {
        if(count<1)throw new IllegalArgumentException("A carousel needs at least one bulletin");
        this.count=count;this.clock=clock;last=clock.getAsLong();
    }
    void synchronizeClock(){last=clock.getAsLong();}
    void tick(boolean paused) {
        long now=clock.getAsLong(),delta=Math.max(0,now-last);last=now;
        if(count==1)return;
        if(paused) {
            // Finish an in-progress/manual fade, then hold the image until the pointer leaves.
            if(fading){elapsed+=delta;if(elapsed>=FADE_NANOS){current=target;fading=false;elapsed=0;}}
            return;
        }
        elapsed+=delta;
        while(elapsed>=(fading?FADE_NANOS:HOLD_NANOS)) {
            elapsed-=fading?FADE_NANOS:HOLD_NANOS;
            if(fading){current=target;fading=false;}
            else {target=(current+1)%count;fading=true;}
        }
    }
    void select(int page) {
        tick(true);
        page=Math.floorMod(page,count);
        if(fading&&page==target)return;
        if(fading&&progress()>=.5f)current=target;
        target=page;elapsed=0;fading=target!=current;
    }
    void step(int direction){select(selected()+direction);}
    int current(){return current;}
    int target(){return fading?target:current;}
    int selected(){return fading?target:current;}
    float progress() {
        if(!fading)return 0;
        float t=Math.clamp(elapsed/(float)FADE_NANOS,0,1);
        return t*t*(3-2*t);
    }
}
