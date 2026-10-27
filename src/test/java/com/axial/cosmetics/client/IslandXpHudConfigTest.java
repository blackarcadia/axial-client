package com.axial.cosmetics.client;

import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class IslandXpHudConfigTest {
    @TempDir Path directory;
    @Test void originalCentrePositionIsRetainedUntilMovedThenSurvivesRestart() {
        Path file=directory.resolve("xp.json");IslandXpHudConfig.load(file);
        assertEquals(278,IslandXpHudConfig.x(640,84));assertEquals(186,IslandXpHudConfig.y(360,22));
        IslandXpHudConfig.setPosition(42,75);IslandXpHudConfig.save(file);
        IslandXpHudConfig.load(directory.resolve("other.json"));
        IslandXpHudConfig.load(file);
        assertEquals(42,IslandXpHudConfig.x(640,84));assertEquals(75,IslandXpHudConfig.y(360,22));
    }
    @Test void smallerWindowClampsWithoutOverwritingSavedPosition() {
        IslandXpHudConfig.setPosition(500,240);
        assertEquals(236,IslandXpHudConfig.x(320,84));assertEquals(198,IslandXpHudConfig.y(220,22));
        assertEquals(500,IslandXpHudConfig.x(1000,84));assertEquals(240,IslandXpHudConfig.y(600,22));
    }
    @Test void partialOrInvalidFileKeepsDefaultLocation() throws Exception {
        Path file=directory.resolve("xp.json");
        for(String json:new String[]{"{}","null","{bad","{\"x\":10}","{\"x\":-4,\"y\":50}"}) {
            Files.writeString(file,json);IslandXpHudConfig.load(file);
            assertEquals(278,IslandXpHudConfig.x(640,84));assertEquals(186,IslandXpHudConfig.y(360,22));
        }
    }
}
