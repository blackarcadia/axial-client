package org.example.launcher;

import java.nio.file.Path;
import java.util.Locale;

final class ClientPaths {
    private ClientPaths() {
    }

    static Path appRoot() {
        if (isWindows()) {
            String appData = System.getenv("APPDATA");
            if (appData != null && !appData.isBlank()) {
                return Path.of(appData, "AxialLauncher");
            }
            return Path.of(System.getProperty("user.home"), "AppData", "Roaming", "AxialLauncher");
        }

        if (isMac()) {
            return Path.of(System.getProperty("user.home"), "Library", "Application Support", "AxialLauncher");
        }

        String xdgDataHome = System.getenv("XDG_DATA_HOME");
        if (xdgDataHome != null && !xdgDataHome.isBlank()) {
            return Path.of(xdgDataHome, "AxialLauncher");
        }
        return Path.of(System.getProperty("user.home"), ".local", "share", "AxialLauncher");
    }

    static Path clientRoot() {
        return appRoot().resolve("client");
    }

    static Path accountsDir() {
        return appRoot().resolve("accounts");
    }

    static boolean isMac() {
        return osName().contains("mac");
    }

    static boolean isWindows() {
        return osName().contains("win");
    }

    private static String osName() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
    }
}
