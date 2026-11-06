package com.axial.cosmetics.client;

/** Ballistic motion in blocks and seconds, independent of render frame rate. */
final class HitIndicatorAnimation {
    static final double DURATION = .8;
    record Frame(double x, double y, double z, float scale, int color) {}
    static Frame frame(double seconds, double velocityX, double velocityZ) {
        double t = Math.clamp(seconds, 0, DURATION);
        float alpha = (float) Math.clamp((DURATION - t) / .3, 0, 1);
        float pop = (float) Math.sin(Math.min(1, t / .16) * Math.PI);
        return new Frame(velocityX * t, 1.25 * t - 2.8 * t * t, velocityZ * t,
                .23f + .055f * pop, Math.round(alpha * 255) << 24 | 0xFF5555);
    }
    private HitIndicatorAnimation() {}
}
