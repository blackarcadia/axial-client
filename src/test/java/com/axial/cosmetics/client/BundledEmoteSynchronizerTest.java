package com.axial.cosmetics.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BundledEmoteSynchronizerTest {
    @Test
    void refreshesBundledEmotesAndIconsForExistingInstallations(@TempDir Path gameDirectory) throws Exception {
        Path emotes = gameDirectory.resolve("emotes");
        Files.createDirectories(emotes);
        Files.writeString(emotes.resolve("keep_your_head_off.png"), "old icon");

        BundledEmoteSynchronizer.synchronize(gameDirectory);

        Path icon = emotes.resolve("keep_your_head_off.png");
        assertTrue(Files.isRegularFile(emotes.resolve("keep_your_head_off.emotecraft")));
        assertTrue(Files.isRegularFile(icon));
        try (var bundled = getClass().getResourceAsStream("/emotes/keep_your_head_off.png")) {
            assertEquals(-1L, Files.mismatch(icon, Files.write(gameDirectory.resolve("expected.png"), bundled.readAllBytes())));
        }
    }
}
