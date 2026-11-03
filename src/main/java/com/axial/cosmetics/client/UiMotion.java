package com.axial.cosmetics.client;

import java.util.function.LongSupplier;

/** Monotonic time-based easing; target changes reverse continuously without overshoot. */
final class UiMotion {
    private final LongSupplier clock;
    private long last;
    private double value;
    private final double speed;
    UiMotion(double initial) { this(initial, 22, System::nanoTime); }
    UiMotion(double initial, double speed, LongSupplier clock) {
        this.value=initial;this.speed=speed;this.clock=clock;last=clock.getAsLong();
    }
    double toward(double target) {
        long now=clock.getAsLong();double dt=Math.max(0,(now-last)/1e9);last=now;
        value=target+(value-target)*Math.exp(-speed*dt);
        if(Math.abs(value-target)<.01)value=target;
        return value;
    }
    void jump(double next) {value=next;last=clock.getAsLong();}
}
