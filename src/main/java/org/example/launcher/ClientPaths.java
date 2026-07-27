package org.example.launcher;

import java.nio.file.Path;

final class ClientPaths {
    private ClientPaths() {
    }

    static Path appRoot() {
        return Path.of(System.getProperty("user.home"), "Library", "Application Support", "AxialLauncher");
    }

    static Path clientRoot() {
        return appRoot().resolve("client");
    }

    static Path accountsDir() {
        return appRoot().resolve("accounts");
    }

    static Path clientReadyMarker() {
        return clientRoot().resolve(".client-ready");
    }
}
