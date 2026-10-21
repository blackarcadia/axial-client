package com.axial.cosmetics.client;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/** Keeps Axial's bundled Emotecraft files current after an automatic mod update. */
public final class BundledEmoteSynchronizer {
    private static final List<String> EMOTES = List.of(
            "gangnam_style.emotecraft",
            "rat_dance.emotecraft",
            "death.emotecraft",
            "fear.emotecraft",
            "keep_your_head_off.emotecraft",
            "spooky_dance.emotecraft"
    );

    private BundledEmoteSynchronizer() {
    }

    public static void synchronize() {
        try {
            synchronize(FabricLoader.getInstance().getGameDir());
        } catch (IOException exception) {
            System.err.println("Could not update bundled Axial emotes: " + exception.getMessage());
        }
    }

    static void synchronize(Path gameDirectory) throws IOException {
        Path emotesDirectory = gameDirectory.resolve("emotes");
        Files.createDirectories(emotesDirectory);

        for (String emoteFile : EMOTES) {
            copyResourceIfChanged("/emotes/" + emoteFile, emotesDirectory.resolve(emoteFile));

            String iconFile = emoteFile.substring(0, emoteFile.lastIndexOf('.')) + ".png";
            copyResourceIfPresent("/emotes/" + iconFile, emotesDirectory.resolve(iconFile));
        }
    }

    private static void copyResourceIfPresent(String resource, Path target) throws IOException {
        try (InputStream input = BundledEmoteSynchronizer.class.getResourceAsStream(resource)) {
            if (input != null) {
                copyIfChanged(input, target);
            }
        }
    }

    private static void copyResourceIfChanged(String resource, Path target) throws IOException {
        try (InputStream input = BundledEmoteSynchronizer.class.getResourceAsStream(resource)) {
            if (input == null) {
                throw new IOException("Missing bundled resource " + resource);
            }
            copyIfChanged(input, target);
        }
    }

    private static void copyIfChanged(InputStream input, Path target) throws IOException {
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        Files.copy(input, temporary, StandardCopyOption.REPLACE_EXISTING);
        if (Files.exists(target) && Files.mismatch(temporary, target) == -1L) {
            Files.deleteIfExists(temporary);
            return;
        }

        try {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicMoveFailure) {
            try {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException replacementFailure) {
                Files.deleteIfExists(temporary);
                throw replacementFailure;
            }
        }
    }
}
