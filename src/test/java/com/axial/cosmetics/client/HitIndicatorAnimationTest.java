package com.axial.cosmetics.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HitIndicatorAnimationTest {
    @Test void popsUpThenFallsBelowItsSpawnPoint() {
        var start = HitIndicatorAnimation.frame(0, .4, -.2);
        var rise = HitIndicatorAnimation.frame(.15, .4, -.2);
        var fall = HitIndicatorAnimation.frame(.7, .4, -.2);
        assertEquals(0, start.y());
        assertTrue(rise.y() > 0);
        assertTrue(fall.y() < 0);
        assertEquals(.28, fall.x(), 1e-6);
        assertEquals(-.14, fall.z(), 1e-6);
        assertTrue(HitIndicatorAnimation.frame(.08, 0, 0).scale() > start.scale());
    }
    @Test void holdsMinecraftLightRedThenFadesOutCompletely() {
        assertEquals(0xFFFF5555, HitIndicatorAnimation.frame(0, 0, 0).color());
        assertEquals(255, HitIndicatorAnimation.frame(.4, 0, 0).color() >>> 24);
        assertTrue((HitIndicatorAnimation.frame(.65, 0, 0).color() >>> 24) < 255);
        assertEquals(0, HitIndicatorAnimation.frame(.8, 0, 0).color() >>> 24);
        assertEquals(0, HitIndicatorAnimation.frame(20, 0, 0).color() >>> 24);
    }
    @Test void frameSamplingDoesNotChangeTheTrajectory() {
        var expected = HitIndicatorAnimation.frame(.45, .3, -.3);
        for (int frame = 0; frame < 144; frame++) HitIndicatorAnimation.frame(frame / 144.0, .3, -.3);
        assertEquals(expected, HitIndicatorAnimation.frame(.45, .3, -.3));
    }
}
