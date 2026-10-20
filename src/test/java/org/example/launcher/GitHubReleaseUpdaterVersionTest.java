package org.example.launcher;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GitHubReleaseUpdaterVersionTest {
    @TempDir Path directory;

    @Test void ordersReleaseTagsByTheirCiBuildNumber() {
        assertTrue(GitHubReleaseUpdater.isNewer("v1beta.246", "v1beta.245"));
        assertTrue(GitHubReleaseUpdater.isNewer("v1.0.246", "v1beta.245"));
        assertFalse(GitHubReleaseUpdater.isNewer("v1beta.245", "v1.0.246"));
    }

    @Test void rejectsVersionsWithoutAnExplicitBuildNumber() {
        assertFalse(GitHubReleaseUpdater.isNewer("AlphaTest", "v1beta.245"));
        assertFalse(GitHubReleaseUpdater.isNewer("v1beta.245", "AlphaTest"));
        assertTrue(GitHubReleaseUpdater.isNewer("v1beta.245", "1.0.0"));
    }

    @Test void refusesAnAssetWhosePackagedBuildDoesNotMatchItsReleaseTag() throws Exception {
        Path matching = createApp("1.0.245");
        assertDoesNotThrow(() -> GitHubReleaseUpdater.verifyStagedBuild(matching, "v1beta.245"));

        Path stale = createApp("1.0.200");
        assertThrows(IOException.class, () -> GitHubReleaseUpdater.verifyStagedBuild(stale, "v1beta.245"));
    }

    @Test void usesTheInstalledClientJarInsteadOfAStaleReleaseMarker() throws Exception {
        Path gameDir = directory.resolve("client");
        createClientMod(gameDir, "1.0.245");

        assertTrue(GitHubReleaseUpdater.isNewer("v1beta.246", GitHubReleaseUpdater.installedClientBuild(gameDir)));
        assertFalse(GitHubReleaseUpdater.isNewer("v1beta.245", GitHubReleaseUpdater.installedClientBuild(gameDir)));
    }

    private Path createApp(String version) throws IOException {
        Path app = Files.createDirectories(directory.resolve(version).resolve("AxialLauncher.app"));
        Path jar = Files.createDirectories(app.resolve("Contents/app")).resolve("launcher.jar");
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar))) {
            output.putNextEntry(new JarEntry("version.properties"));
            output.write(("app.version=" + version + "\n").getBytes(StandardCharsets.UTF_8));
            output.closeEntry();
        }
        return app;
    }

    private void createClientMod(Path gameDir, String version) throws IOException {
        Path jar = Files.createDirectories(gameDir.resolve("mods")).resolve("axial-cosmetics.jar");
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar))) {
            output.putNextEntry(new JarEntry("version.properties"));
            output.write(("app.version=" + version + "\n").getBytes(StandardCharsets.UTF_8));
            output.closeEntry();
        }
    }
}
