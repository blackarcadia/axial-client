package com.axial.cosmetics.client;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GoldSparkAuraTest {
    @Test void loopIsSeamlessBoundedAndAnimated() {
        for (float t : new float[]{0, .25f, 19, 60, 119.9f, 120, 240, -1}) {
            var mesh = GoldSparkGeometry.frame(t, true);
            assertFalse(mesh.isEmpty());
            assertTrue(mesh.size() < 400);
            for (var quad : mesh) for (var p : List.of(quad.a(), quad.b(), quad.c(), quad.d())) {
                assertTrue(Float.isFinite(p.x()) && Float.isFinite(p.y()) && Float.isFinite(p.z()));
                assertTrue(Math.abs(p.x()) < .9 && Math.abs(p.z()) < .9);
                assertTrue(p.y() > -.2 && p.y() < 2.3);
            }
        }
        assertEquals(GoldSparkGeometry.frame(0, true), GoldSparkGeometry.frame(120, true));
        assertNotEquals(GoldSparkGeometry.frame(0, true), GoldSparkGeometry.frame(1, true));
        assertTrue(GoldSparkGeometry.frame(0, false).size() < GoldSparkGeometry.frame(0, true).size());
    }
    @Test void trailsRiseOverTime() {
        // At tick zero, the first spark in the mesh is already alive across the loop boundary.
        var before = GoldSparkGeometry.frame(0, true).getFirst();
        var after = GoldSparkGeometry.frame(.1f, true).getFirst();
        assertEquals(before.a().x(), after.a().x());
        assertTrue(after.a().y() > before.a().y());
    }
    @Test void catalogueAndPreviewAreAvailable() throws Exception {
        assertEquals(WardrobeAura.GOLD_SPARK, WardrobeAura.fromId("gold_spark"));
        assertNotEquals(WardrobeAura.HEXWIND.preview(), WardrobeAura.GOLD_SPARK.preview());
        try (var input = getClass().getResourceAsStream("/assets/axial_cosmetics/textures/gui/gold_spark_preview.png")) {
            assertNotNull(input);
            var atlas = javax.imageio.ImageIO.read(input);
            assertEquals(1024, atlas.getWidth());
            assertEquals(1024, atlas.getHeight());
            boolean animated = false;
            for (int y = 0; y < 128; y++) for (int x = 0; x < 128; x++)
                animated |= atlas.getRGB(x, y) != atlas.getRGB(x + 128, y);
            assertTrue(animated);
        }
    }
}
