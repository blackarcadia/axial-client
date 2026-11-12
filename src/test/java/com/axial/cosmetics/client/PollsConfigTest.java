package com.axial.cosmetics.client;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class PollsConfigTest {
    @TempDir Path directory;
    @Test void movedPositionAndToggleSurviveReloadAndFitSmallerWindows() throws Exception {
        var file=directory.resolve("polls.json");
        java.nio.file.Files.writeString(file,"{\"enabled\":false,\"x\":350,\"y\":200}");
        PollsConfig.load(file);assertFalse(PollsConfig.enabled());
        assertEquals(82,PollsConfig.x(320,238));assertEquals(72,PollsConfig.y(180,108));
        PollsConfig.setPosition(20,30);PollsConfig.save(file);PollsConfig.load(file);
        assertEquals(20,PollsConfig.x(640,238));assertEquals(30,PollsConfig.y(360,108));
    }
}
