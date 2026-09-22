package com.axial.cosmetics.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PotionsHudConfigTest {
    @TempDir Path directory;

    @Test
    void settingsAndDraggedPositionSurviveReload() throws Exception {
        Path file = directory.resolve("potions.json");
        Files.writeString(file, """
                {"enabled":true,"box":false,"title":"My Effects","titleColor":1193046,"x":34,"y":57}
                """);
        PotionsHudConfig.load(file);
        assertTrue(PotionsHudConfig.isEnabled());
        assertFalse(PotionsHudConfig.showBox());
        assertEquals(0xFF123456, PotionsHudConfig.titleColor());
        PotionsHudConfig.setPosition(200, 120);
        PotionsHudConfig.setTitle("Active Potions");
        PotionsHudConfig.save(file);
        PotionsHudConfig.load(file);
        assertEquals("Active Potions", PotionsHudConfig.title());
        assertEquals(200, PotionsHudConfig.getX(640, 150));
        assertEquals(120, PotionsHudConfig.getY(360, 72));
        assertTrue(PotionsHudConfig.isEnabled());
        assertFalse(PotionsHudConfig.showBox());
    }

    @Test
    void malformedOrPartialSettingsKeepHudUsable() throws Exception {
        Path file = directory.resolve("potions.json");
        Files.writeString(file, "{broken");
        PotionsHudConfig.load(file);
        assertFalse(PotionsHudConfig.isEnabled());
        assertTrue(PotionsHudConfig.showBox());
        assertEquals("Potions", PotionsHudConfig.title());
        Files.writeString(file, "{\"title\":null,\"enabled\":true}");
        PotionsHudConfig.load(file);
        assertTrue(PotionsHudConfig.isEnabled());
        assertEquals("Potions", PotionsHudConfig.title());
    }

    @Test
    void resizingAndGrowingEffectListKeepPositionOnScreen() {
        assertEquals(490, PotionsHudConfig.clampPosition(600, 640, 150));
        assertEquals(288, PotionsHudConfig.clampPosition(340, 360, 72));
        assertEquals(0, PotionsHudConfig.clampPosition(-20, 640, 150));
        assertEquals(0, PotionsHudConfig.clampPosition(20, 100, 150));
    }
}
