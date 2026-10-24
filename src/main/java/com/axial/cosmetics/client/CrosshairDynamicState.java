package com.axial.cosmetics.client;

public final class CrosshairDynamicState {
    private static final long PULSE_DURATION_MS = 220L;
    private static final float MAX_BOOST = 0.42f;
    private static volatile long lastPulseNanos = -1L;

    private CrosshairDynamicState() {
    }

    public static void triggerPulse() {
        lastPulseNanos = System.nanoTime();
    }

    public static float scaleMultiplier() {
        long pulseStart = lastPulseNanos;
        if (pulseStart == -1L) {
            return 1.0f;
        }

        return atElapsedMillis((System.nanoTime()-pulseStart)/1_000_000L);
    }

    static float atElapsedMillis(long elapsed) {
        if(elapsed<0||elapsed>=PULSE_DURATION_MS)return 1;
        float progress = Math.max(0.0f, Math.min(1.0f, elapsed / (float) PULSE_DURATION_MS));
        float pulse = (float) Math.sin(progress * Math.PI);
        float wobble = (float) Math.sin(progress * Math.PI * 3.0f) * 0.10f * (1.0f - progress);
        return 1.0f + (MAX_BOOST * pulse) + wobble;
    }
}
