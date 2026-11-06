package com.axial.cosmetics.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HitDamageTrackerTest {
    @Test void healthLossCanArriveBeforeOrAfterTheHitPacket() {
        var tracker = new HitDamageTracker();
        assertTrue(tracker.hit(1, 0).isEmpty());
        assertEquals(10, tracker.loss(1, 10, .05).orElseThrow());
        assertTrue(tracker.loss(2, 3.5, .1).isEmpty());
        assertEquals(3.5, tracker.hit(2, .15).orElseThrow());
    }
    @Test void unrelatedTargetsAndExpiredLossesDoNotCreateNumbers() {
        var tracker = new HitDamageTracker();
        tracker.hit(1, 0);
        assertTrue(tracker.loss(2, 5, .05).isEmpty());
        assertTrue(tracker.hit(2, 1).isEmpty());
        assertTrue(tracker.loss(1, 5, 1).isEmpty());
    }
    @Test void consumesEachMatchOnceAndClearsAcrossWorlds() {
        var tracker = new HitDamageTracker();
        tracker.hit(1, 0);
        assertEquals(4, tracker.loss(1, 4, .05).orElseThrow());
        assertTrue(tracker.loss(1, 4, .1).isEmpty());
        tracker.clear();
        assertTrue(tracker.hit(1, .15).isEmpty());
    }
    @Test void healingAndInvalidHealthValuesAreIgnored() {
        var tracker = new HitDamageTracker();
        tracker.hit(1, 0);
        for (double value : new double[]{0, -5, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertTrue(tracker.loss(1, value, .05).isEmpty());
        }
        assertEquals(2, tracker.loss(1, 2, .1).orElseThrow());
    }
    @Test void formatsHpWithoutUnnecessaryDecimalZeros() {
        assertEquals("- 10 HP", HitDamageTracker.label(10));
        assertEquals("- 3.5 HP", HitDamageTracker.label(3.5));
        assertEquals("- 1.3 HP", HitDamageTracker.label(1.26));
    }
}
