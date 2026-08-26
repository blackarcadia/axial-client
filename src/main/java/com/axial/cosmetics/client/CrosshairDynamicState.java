package com.axial.cosmetics.client;

public final class CrosshairDynamicState {
    private static final long PULSE_DURATION_MS = 220L;
    private static final float MAX_BOOST = 0.42f;
    private static volatile long lastPulseMs = -1L;

    private CrosshairDynamicState() {
    }

    public static void triggerPulse() {
        lastPulseMs = System.currentTimeMillis();
    }

    public static float scaleMultiplier() {
        long pulseStart = lastPulseMs;
        if (pulseStart < 0L) {
            return 1.0f;
        }

        long elapsed = System.currentTimeMillis() - pulseStart;
        if (elapsed >= PULSE_DURATION_MS) {
            return 1.0f;
        }

        float progress = Math.max(0.0f, Math.min(1.0f, elapsed / (float) PULSE_DURATION_MS));
        float pulse = (float) Math.sin(progress * Math.PI);
        float wobble = (float) Math.sin(progress * Math.PI * 3.0f) * 0.10f * (1.0f - progress);
        return 1.0f + (MAX_BOOST * pulse) + wobble;
    }
}
