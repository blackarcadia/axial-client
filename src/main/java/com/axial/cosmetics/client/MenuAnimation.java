package com.axial.cosmetics.client;

import java.util.function.LongSupplier;

/** Time-based motion, independent of render frame rate. */
final class MenuAnimation {
    private final LongSupplier clock;
    private long started;
    private float from;
    private boolean closing;

    MenuAnimation() { this(System::nanoTime); }
    MenuAnimation(LongSupplier clock) { this.clock=clock; started=clock.getAsLong(); }

    float progress() {
        float t=Math.clamp((clock.getAsLong()-started)/(closing?140_000_000f:180_000_000f),0f,1f);
        float eased=1-(1-t)*(1-t)*(1-t);
        return from+((closing?0f:1f)-from)*eased;
    }

    void close() {
        if(closing)return;
        from=progress(); started=clock.getAsLong(); closing=true;
    }

    boolean closing() { return closing; }
    boolean finished() { return closing && clock.getAsLong()-started>=140_000_000L; }
    float offset() { return (1-progress())*10; }

    static float hover(float current,boolean hovered,float seconds) {
        float weight=1-(float)Math.exp(-14*Math.clamp(seconds,0f,.1f));
        return current+((hovered?1:0)-current)*weight;
    }
}
