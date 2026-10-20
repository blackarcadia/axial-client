package org.example.launcher;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.DataInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class EmotecraftBundledEmoteTest {
    @Test void ratDanceEmoteAndItsIconArePackagedAndReadable() throws Exception {
        try (var emote = getClass().getResourceAsStream("/emotes/rat_dance.emotecraft");
             var icon = getClass().getResourceAsStream("/emotes/rat_dance.png")) {
            assertNotNull(emote);
            assertNotNull(icon);
            assertEquals(8, new DataInputStream(emote).readInt());
            var image = ImageIO.read(icon);
            assertNotNull(image);
            assertEquals(256, image.getWidth());
            assertEquals(256, image.getHeight());
        }
    }

    @Test void deathEmoteAndItsIconArePackagedAndReadable() throws Exception {
        try (var emote = getClass().getResourceAsStream("/emotes/death.emotecraft");
             var icon = getClass().getResourceAsStream("/emotes/death.png")) {
            assertNotNull(emote);
            assertNotNull(icon);
            assertEquals(8, new DataInputStream(emote).readInt());
            var image = ImageIO.read(icon);
            assertNotNull(image);
            assertEquals(256, image.getWidth());
            assertEquals(256, image.getHeight());
        }
    }

    @Test void fearEmoteAndItsIconArePackagedAndReadable() throws Exception {
        try (var emote = getClass().getResourceAsStream("/emotes/fear.emotecraft");
             var icon = getClass().getResourceAsStream("/emotes/fear.png")) {
            assertNotNull(emote);
            assertNotNull(icon);
            assertEquals(8, new DataInputStream(emote).readInt());
            var image = ImageIO.read(icon);
            assertNotNull(image);
            assertEquals(256, image.getWidth());
            assertEquals(256, image.getHeight());
        }
    }

    @Test void keepYourHeadOffEmoteAndItsIconArePackagedAndReadable() throws Exception {
        try (var emote = getClass().getResourceAsStream("/emotes/keep_your_head_off.emotecraft");
             var icon = getClass().getResourceAsStream("/emotes/keep_your_head_off.png")) {
            assertNotNull(emote);
            assertNotNull(icon);
            assertEquals(8, new DataInputStream(emote).readInt());
            var image = ImageIO.read(icon);
            assertNotNull(image);
            assertEquals(256, image.getWidth());
            assertEquals(256, image.getHeight());
        }
    }
}
