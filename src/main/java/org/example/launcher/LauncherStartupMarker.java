package org.example.launcher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

public final class LauncherStartupMarker {
    private static final Path MARKER_PATH = Path.of(
            System.getProperty("user.home"),
            "Library",
            "Application Support",
            "AxialLauncher",
            "launcher-startup.marker"
    );

    private LauncherStartupMarker() {
    }

    public static Path markerPath() {
        return MARKER_PATH;
    }

    public static void markStarted() {
        try {
            Files.createDirectories(MARKER_PATH.getParent());
            Files.writeString(MARKER_PATH, Instant.now().toString() + System.lineSeparator());
        } catch (IOException ignored) {
        }
    }
}
