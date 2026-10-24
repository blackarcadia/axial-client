package com.axial.cosmetics.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class PerformanceConfigTest {
    @TempDir Path directory;
    @Test void defaultsPreserveExistingWorldRendering(){
        PerformanceConfig.load(directory.resolve("missing.json"));
        assertFalse(PerformanceConfig.displayFps());assertFalse(PerformanceConfig.disableSpawnerMobs());
        assertTrue(PerformanceConfig.droppedItems());assertTrue(PerformanceConfig.liquids());
        assertTrue(PerformanceConfig.particles());assertTrue(PerformanceConfig.weather());
        assertEquals(0xFFFFFFFF,PerformanceConfig.fpsColor());
    }
    @Test void renderingPreferencesAndOpaqueFpsColorSurviveReload()throws Exception{
        Path path=directory.resolve("performance.json");
        Files.writeString(path,"""
            {"displayFps":true,"droppedItems":false,"liquids":false,"disableSpawnerMobs":true,
             "particles":false,"weather":false,"fpsColor":1193046}
            """);
        PerformanceConfig.load(path);PerformanceConfig.save(path);
        PerformanceConfig.load(directory.resolve("missing.json"));PerformanceConfig.load(path);
        assertTrue(PerformanceConfig.displayFps());assertTrue(PerformanceConfig.disableSpawnerMobs());
        assertFalse(PerformanceConfig.droppedItems());assertFalse(PerformanceConfig.liquids());
        assertFalse(PerformanceConfig.particles());assertFalse(PerformanceConfig.weather());
        assertEquals(0xFF123456,PerformanceConfig.fpsColor());
    }
    @Test void partialOrMalformedFilesFallBackToNormalRendering()throws Exception{
        Path path=directory.resolve("performance.json");Files.writeString(path,"{\"displayFps\":true}");
        PerformanceConfig.load(path);assertTrue(PerformanceConfig.displayFps());assertTrue(PerformanceConfig.liquids());
        Files.writeString(path,"{invalid");PerformanceConfig.load(path);
        assertFalse(PerformanceConfig.displayFps());assertTrue(PerformanceConfig.droppedItems());
    }
}
