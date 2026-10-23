package com.axial.cosmetics.client;

import java.util.function.LongSupplier;

/** Critically damped motion: fast to respond, with no bounce or jump on reversal. */
final class ToggleAnimation {
    private static final double SPEED = 24;
    private final LongSupplier clock;
    private long last;
    private double position, velocity, target;

    ToggleAnimation(boolean enabled) { this(enabled, System::nanoTime); }
    ToggleAnimation(boolean enabled, LongSupplier clock) {
        this.clock = clock;
        position = target = enabled ? 1 : 0;
        last = clock.getAsLong();
    }

    float value(boolean enabled) {
        long now = clock.getAsLong();
        double seconds = Math.max(0, (now - last) / 1_000_000_000.0);
        last = now;
        // Exact spring integration keeps the same motion at every frame rate.
        double error = position - target;
        double coefficient = velocity + SPEED * error;
        double decay = Math.exp(-SPEED * seconds);
        position = target + (error + coefficient * seconds) * decay;
        velocity = (velocity - SPEED * coefficient * seconds) * decay;
        target = enabled ? 1 : 0;
        return (float) Math.clamp(position, 0, 1);
    }
}
