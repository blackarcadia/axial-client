package org.example.launcher;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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

    @Test void refusesANewLauncherContainingAnOldClientPayload() throws Exception {
        Path stale = createApp("1.0.246", "1.0.200");
        assertThrows(IOException.class, () -> GitHubReleaseUpdater.verifyStagedBuild(stale, "v1beta.246"));
    }

    @Test void preservesDownloadedClientAndDependenciesAcrossRepeatedLaunches() throws Exception {
        Path gameDir = directory.resolve("client");
        createClientMod(gameDir, "1.0.999999999");
        Path clientJar = gameDir.resolve("mods/axial-cosmetics.jar");
        byte[] original = Files.readAllBytes(clientJar);
        Path dependency = gameDir.resolve("mods/geckolib-fabric-newer.jar");
        Files.writeString(dependency, "downloaded dependency");

        for (int launch = 0; launch < 3; launch++) {
            // A fresh launcher instance, as on each subsequent application startup.
            new MinecraftLauncher(message -> {}).installBundledClient(gameDir);
            assertArrayEquals(original, Files.readAllBytes(clientJar));
            assertEquals("downloaded dependency", Files.readString(dependency));
        }
    }

    @Test void rejectsAnUpdateThatBecameOlderThanTheInstalledClient() throws Exception {
        Path gameDir = directory.resolve("client");
        createClientMod(gameDir, "1.0.247");
        Path staged = createApp("1.0.246");
        var updater = new GitHubReleaseUpdater(new AppBuildInfo("1.0.200", "release", "owner", "repo", "app.zip"));
        assertThrows(IOException.class, () -> updater.installClientUpdate(staged, "v1beta.246", gameDir));
        assertEquals("1.0.247", GitHubReleaseUpdater.installedClientBuild(gameDir));
    }

    @Test void extractsLauncherOnlyDependenciesAlongsideTheClient() throws Exception {
        Path mods = Files.createDirectories(directory.resolve("game/mods"));
        Path bundle = directory.resolve("bundle.jar");
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(bundle))) {
            output.putNextEntry(new JarEntry("axial-cosmetics.jar"));
            output.write(clientModBytes("1.0.246"));
            output.closeEntry();
            output.putNextEntry(new JarEntry("launcher-mods/BendableCuboidsFabric-new.jar"));
            output.write("dependency".getBytes(StandardCharsets.UTF_8));
            output.closeEntry();
        }
        try (JarFile jar = new JarFile(bundle.toFile())) {
            var installed = GitHubReleaseUpdater.installPackagedJarsFromBundle(jar, mods);
            assertTrue(installed.contains("BendableCuboidsFabric-new.jar"));
        }
        assertEquals("dependency", Files.readString(mods.resolve("BendableCuboidsFabric-new.jar")));
        assertEquals("1.0.246", GitHubReleaseUpdater.installedClientBuild(mods.getParent()));
    }

    @Test void extractionFailureLeavesTheWorkingClientUntouched() throws Exception {
        Path game = directory.resolve("game");
        createClientMod(game, "1.0.245");
        Path bundle = directory.resolve("incomplete.jar");
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(bundle))) {
            output.putNextEntry(new JarEntry("dependency.jar"));
            output.write(1);
            output.closeEntry();
        }
        try (JarFile jar = new JarFile(bundle.toFile())) {
            assertThrows(IOException.class, () -> GitHubReleaseUpdater.installPackagedJarsFromBundle(jar, game.resolve("mods")));
        }
        assertEquals("1.0.245", GitHubReleaseUpdater.installedClientBuild(game));
    }

    @Test void downloadsCurrentReleaseWhenLauncherIsCurrentButGameWasDowngraded() throws Exception {
        var updater = new GitHubReleaseUpdater(new AppBuildInfo("1.0.246", "release", "owner", "repo", "app.zip"));
        Path game = directory.resolve("downgraded");
        createClientMod(game, "1.0.200");
        var status = updater.updateStatusForRelease("v1beta.246", URI.create("https://example.com/app.zip"), game);
        assertTrue(status.available());
        assertFalse(status.upToDate());
        assertDoesNotThrow(() -> updater.verifyInstallVersion("v1beta.246", game));
    }

    @Test void downloadsCurrentReleaseWhenClientIsLegacyMissingOrCorrupt() throws Exception {
        var updater = new GitHubReleaseUpdater(new AppBuildInfo("1.0.246", "release", "owner", "repo", "app.zip"));
        Path game = directory.resolve("legacy");
        createClientMod(game, "AlphaTest");
        for (int state = 0; state < 3; state++) {
            if (state == 1) Files.writeString(game.resolve("mods/axial-cosmetics.jar"), "broken jar");
            if (state == 2) Files.delete(game.resolve("mods/axial-cosmetics.jar"));
            var status = updater.updateStatusForRelease("v1beta.246", URI.create("https://example.com/app.zip"), game);
            assertTrue(status.available());
            assertFalse(status.upToDate());
            assertDoesNotThrow(() -> updater.verifyInstallVersion("v1beta.246", game));
        }
    }

    @Test void reportsCurrentOnlyFromTheActualInstalledClient() throws Exception {
        var updater = new GitHubReleaseUpdater(new AppBuildInfo("1.0.200", "release", "owner", "repo", "app.zip"));
        Path game = directory.resolve("current");
        createClientMod(game, "1.0.247");
        var status = updater.updateStatusForRelease("v1beta.246", URI.create("https://example.com/app.zip"), game);
        assertTrue(status.upToDate());
        assertFalse(status.available());
        assertEquals("1.0.247", status.version());
        assertThrows(IOException.class, () -> updater.verifyInstallVersion("v1beta.246", game));
    }

    @Test void doesNotDownloadAReleaseOlderThanTheLauncherToRepairAnOldClient() throws Exception {
        var updater = new GitHubReleaseUpdater(new AppBuildInfo("1.0.247", "release", "owner", "repo", "app.zip"));
        Path game = directory.resolve("old");
        createClientMod(game, "1.0.200");
        var status = updater.updateStatusForRelease("v1beta.246", URI.create("https://example.com/app.zip"), game);
        assertFalse(status.upToDate());
        assertFalse(status.available());
        assertThrows(IOException.class, () -> updater.verifyInstallVersion("v1beta.246", game));
    }

    private Path createApp(String version) throws IOException {
        return createApp(version, version);
    }

    private Path createApp(String version, String payloadVersion) throws IOException {
        Path app = Files.createDirectories(directory.resolve(version).resolve("AxialLauncher.app"));
        Path jar = Files.createDirectories(app.resolve("Contents/app")).resolve("launcher.jar");
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar))) {
            output.putNextEntry(new JarEntry("version.properties"));
            output.write(("app.version=" + version + "\n").getBytes(StandardCharsets.UTF_8));
            output.closeEntry();
            output.putNextEntry(new JarEntry("axial-cosmetics.jar"));
            output.write(clientModBytes(payloadVersion));
            output.closeEntry();
        }
        return app;
    }

    private byte[] clientModBytes(String version) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (JarOutputStream output = new JarOutputStream(bytes)) {
            output.putNextEntry(new JarEntry("version.properties"));
            output.write(("app.version=" + version + "\n").getBytes(StandardCharsets.UTF_8));
            output.closeEntry();
        }
        return bytes.toByteArray();
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
