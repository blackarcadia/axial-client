package org.example.launcher;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class ClientUpdateSafetyTest {
    @TempDir Path directory;
    private static final URI ASSET = URI.create("https://example.com/client.zip");

    private MinecraftLauncher oldLauncher() {
        return new MinecraftLauncher(s -> {}, new AppBuildInfo("1.0.100", "release", "owner", "repo", "app.zip"));
    }

    private GitHubReleaseUpdater updater(long build) {
        return updater(build, new OkHttpClient());
    }

    private GitHubReleaseUpdater updater(long build, OkHttpClient http) {
        return new GitHubReleaseUpdater(new AppBuildInfo("1.0." + build, "release", "owner", "repo", "app.zip"),
                http, directory.resolve("downloads"));
    }

    private byte[] mod(String version) throws IOException {
        return jar(List.of("version.properties", "client.class"), List.of(
                ("app.version=" + version + "\n").getBytes(StandardCharsets.UTF_8), new byte[]{1, 2, 3}));
    }

    private byte[] jar(List<String> names, List<byte[]> bytes) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (JarOutputStream zip = new JarOutputStream(output)) {
            for (int i = 0; i < names.size(); i++) {
                zip.putNextEntry(new JarEntry(names.get(i)));
                zip.write(bytes.get(i));
                zip.closeEntry();
            }
        }
        return output.toByteArray();
    }

    private Path app(long build) throws IOException {
        Path app = Files.createDirectories(directory.resolve("app-" + build));
        Path appDir = Files.createDirectories(app.resolve("app"));
        Files.write(appDir.resolve("client-all.jar"), jar(List.of("version.properties", "axial-cosmetics.jar", "dependency.jar"),
                List.of(("app.version=1.0." + build + "\n").getBytes(StandardCharsets.UTF_8),
                        mod("1.0." + build), ("dependency-" + build).getBytes(StandardCharsets.UTF_8))));
        return app;
    }

    private Path game(long build) throws IOException {
        Path game = Files.createTempDirectory(directory, "game-");
        Files.createDirectory(game.resolve("mods"));
        Files.write(game.resolve("mods/axial-cosmetics.jar"), mod("1.0." + build));
        return game;
    }

    @Test void remembersHighestBuildAfterClientIsDeletedCorruptedOrReplaced() throws Exception {
        for (String damage : List.of("deleted", "corrupt", "replaced")) {
            Path game = game(500);
            oldLauncher().installBundledClient(game);
            Path client = game.resolve("mods/axial-cosmetics.jar");
            switch (damage) {
                case "deleted" -> Files.delete(client);
                case "corrupt" -> Files.writeString(client, "broken");
                case "replaced" -> Files.write(client, mod("1.0.100"));
            }
            assertThrows(IOException.class, () -> oldLauncher().installBundledClient(game), damage);
            assertThrows(IOException.class, () -> ClientInstallation.locked(game, () -> {
                ClientInstallation.requireLaunchable(game);
                return null;
            }), damage);
            assertFalse(updater(100).updateStatusForRelease("1.0.499", ASSET, game).available());
            assertTrue(updater(100).updateStatusForRelease("1.0.500", ASSET, game).available());
            updater(100).installClientUpdate(app(500), "1.0.500", game);
            assertEquals("1.0.500", GitHubReleaseUpdater.installedClientBuild(game));
            assertEquals(500, ClientInstallation.minimumBuild(game));
        }
    }

    @Test void damagedVersionRecordStopsFallbackWithoutChangingClient() throws Exception {
        Path game = game(500);
        byte[] before = Files.readAllBytes(game.resolve("mods/axial-cosmetics.jar"));
        Files.writeString(game.resolve(".client-minimum-build"), "broken");
        assertThrows(IOException.class, () -> oldLauncher().installBundledClient(game));
        assertThrows(IOException.class, () -> updater(100).installClientUpdate(app(501), "1.0.501", game));
        assertArrayEquals(before, Files.readAllBytes(game.resolve("mods/axial-cosmetics.jar")));
    }

    @Test void randomizedUpdateAndRestartSequencesNeverDecreaseBuild() throws Exception {
        Random random = new Random(0xA71A1);
        for (int simulation = 0; simulation < 20; simulation++) {
            Path game = game(200);
            long highest = 200;
            for (int event = 0; event < 50; event++) {
                long candidate = 190 + random.nextInt(35);
                if (random.nextBoolean()) {
                    oldLauncher().installBundledClient(game);
                } else if (candidate > highest) {
                    updater(100).installClientUpdate(app(candidate), "1.0." + candidate, game);
                    highest = candidate;
                } else {
                    Path staged = app(candidate);
                    assertThrows(IOException.class, () -> updater(100).installClientUpdate(staged, "1.0." + candidate, game));
                }
                assertEquals(highest, GitHubReleaseUpdater.buildNumber(GitHubReleaseUpdater.installedClientBuild(game)),
                        "simulation " + simulation + ", event " + event);
                assertEquals(highest, ClientInstallation.minimumBuild(game));
            }
        }
    }

    @Test void concurrentUpdateAndBundledInstallAttemptsKeepTheHighestBuild() throws Exception {
        Path game = game(199);
        List<Long> builds = new ArrayList<>();
        for (long build = 200; build < 232; build++) {
            app(build);
            builds.add(build);
        }
        Collections.shuffle(builds, new Random(82));
        CountDownLatch start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(8)) {
            var futures = new ArrayList<java.util.concurrent.Future<?>>();
            for (long build : builds) {
                futures.add(pool.submit(() -> {
                    assertTrue(start.await(5, TimeUnit.SECONDS));
                    try {
                        updater(100).installClientUpdate(directory.resolve("app-" + build), "1.0." + build, game);
                    } catch (IOException refused) {
                        assertTrue(refused.getMessage().startsWith("Refusing to replace client build"), refused.toString());
                    }
                    oldLauncher().installBundledClient(game);
                    return null;
                }));
            }
            start.countDown();
            for (var future : futures) future.get(30, TimeUnit.SECONDS);
        }
        assertEquals("1.0.231", GitHubReleaseUpdater.installedClientBuild(game));
        assertEquals("dependency-231", Files.readString(game.resolve("mods/dependency.jar")));
        assertEquals(231, ClientInstallation.minimumBuild(game));
    }

    @Test void separateLauncherProcessCannotRaceAnInstallation() throws Exception {
        Path game = game(200);
        Path ready = directory.resolve("lock-ready");
        Path release = directory.resolve("release-lock");
        String classpath = Path.of(ClientInstallation.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                + java.io.File.pathSeparator
                + Path.of(InstallLockProbe.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Process process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin/java").toString(),
                "-cp", classpath, InstallLockProbe.class.getName(), game.toString(), ready.toString(), release.toString())
                .redirectErrorStream(true).redirectOutput(directory.resolve("probe.log").toFile()).start();
        try (var pool = Executors.newSingleThreadExecutor()) {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (!Files.exists(ready) && process.isAlive() && System.nanoTime() < deadline) Thread.sleep(10);
            assertTrue(Files.exists(ready), () -> "Lock helper failed: " + process.info());
            Path staged = app(201);
            CountDownLatch started = new CountDownLatch(1);
            var install = pool.submit(() -> {
                started.countDown();
                updater(100).installClientUpdate(staged, "1.0.201", game);
                return null;
            });
            assertTrue(started.await(5, TimeUnit.SECONDS));
            try {
                assertThrows(TimeoutException.class, () -> install.get(250, TimeUnit.MILLISECONDS));
                assertEquals("1.0.200", GitHubReleaseUpdater.installedClientBuild(game));
            } finally {
                Files.writeString(release, "release");
            }
            install.get(10, TimeUnit.SECONDS);
            assertTrue(process.waitFor(10, TimeUnit.SECONDS));
            assertEquals(0, process.exitValue());
            assertEquals("1.0.201", GitHubReleaseUpdater.installedClientBuild(game));
        } finally {
            Files.writeString(release, "release");
            process.destroyForcibly();
        }
    }

    // Real second JVM used to test OS-level locking, rather than just Java synchronization.
    public static class InstallLockProbe {
        public static void main(String[] args) throws Exception {
            ClientInstallation.locked(Path.of(args[0]), () -> {
                Files.writeString(Path.of(args[1]), "locked");
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
                while (!Files.exists(Path.of(args[2]))) {
                    if (System.nanoTime() > deadline) throw new IOException("Timed out waiting for test release");
                    try { Thread.sleep(10); }
                    catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IOException(e); }
                }
                return null;
            });
        }
    }

    @Test void midCommitIoFailureRestoresTheExactClientAndDependencies() throws Exception {
        Path game = game(500);
        Files.writeString(game.resolve("mods/dependency.jar"), "old dependency");
        Files.writeString(game.resolve("mods/user-mod.jar"), "user mod");
        Files.writeString(game.resolve(".client-release.files"), "axial-cosmetics.jar\ndependency.jar\n");
        byte[] before = Files.readAllBytes(game.resolve("mods/axial-cosmetics.jar"));
        Path staged = app(501);
        var commits = new java.util.concurrent.atomic.AtomicInteger();
        try (var jar = new java.util.jar.JarFile(staged.resolve("app/client-all.jar").toFile())) {
            assertThrows(IOException.class, () -> GitHubReleaseUpdater.installPackagedJarsFromBundle(jar,
                    game.resolve("mods"), (source, target) -> {
                        if (commits.incrementAndGet() == 2) throw new IOException("Simulated disk/locked-file failure");
                        Files.move(source, target);
                    }));
        }
        assertEquals(2, commits.get());
        assertArrayEquals(before, Files.readAllBytes(game.resolve("mods/axial-cosmetics.jar")));
        assertEquals("old dependency", Files.readString(game.resolve("mods/dependency.jar")));
        assertEquals("user mod", Files.readString(game.resolve("mods/user-mod.jar")));
    }

    @Test void oneGamesVersionRecordDoesNotBlockAnotherGamesRecovery() throws Exception {
        Path first = game(500);
        Path second = game(200);
        oldLauncher().installBundledClient(first);
        assertTrue(updater(100).updateStatusForRelease("1.0.201", ASSET, second).available());
        updater(100).installClientUpdate(app(201), "1.0.201", second);
        assertEquals(500, ClientInstallation.minimumBuild(first));
        assertEquals(201, ClientInstallation.minimumBuild(second));
    }

    @Test void missingOrMalformedClientVersionCannotBeTreatedAsBuildZero() throws Exception {
        Path game = game(500);
        for (String metadata : List.of("client.version=1.0.bffff\n", "app.version=\\uZZZZ\n")) {
            Files.write(game.resolve("mods/axial-cosmetics.jar"), jar(List.of("version.properties"),
                    List.of(metadata.getBytes(StandardCharsets.UTF_8))));
            assertEquals("", GitHubReleaseUpdater.installedClientBuild(game));
            assertTrue(updater(100).updateStatusForRelease("1.0.500", ASSET, game).available());
        }
    }

    @Test void killedInstallerCannotCauseAnOlderBundleFallback() throws Exception {
        Path game = game(500);
        Path ready = directory.resolve("interrupted-ready");
        String classpath = Path.of(ClientInstallation.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                + java.io.File.pathSeparator
                + Path.of(InterruptedInstallProbe.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Process process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin/java").toString(),
                "-cp", classpath, InterruptedInstallProbe.class.getName(), game.toString(), ready.toString())
                .redirectErrorStream(true).redirectOutput(directory.resolve("interrupted.log").toFile()).start();
        try {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (!Files.exists(ready) && process.isAlive() && System.nanoTime() < deadline) Thread.sleep(10);
            assertTrue(Files.exists(ready));
            process.destroyForcibly();
            assertTrue(process.waitFor(10, TimeUnit.SECONDS));
            // The process died between backing up the working client and replacing it.
            assertFalse(Files.exists(game.resolve("mods/axial-cosmetics.jar")));
            assertEquals(500, ClientInstallation.minimumBuild(game));
            assertThrows(IOException.class, () -> oldLauncher().installBundledClient(game));
            assertFalse(updater(100).updateStatusForRelease("1.0.499", ASSET, game).available());
            updater(100).installClientUpdate(app(500), "1.0.500", game);
            assertEquals("1.0.500", GitHubReleaseUpdater.installedClientBuild(game));
        } finally {
            process.destroyForcibly();
        }
    }

    public static class InterruptedInstallProbe {
        public static void main(String[] args) throws Exception {
            Path game = Path.of(args[0]);
            ClientInstallation.locked(game, () -> {
                ClientInstallation.recordBuild(game, 500);
                Files.move(game.resolve("mods/axial-cosmetics.jar"), game.resolve(".interrupted-client-backup"));
                Files.writeString(Path.of(args[1]), "working client backed up");
                try { Thread.sleep(15000); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IOException(e); }
                return null;
            });
        }
    }

    @Test void preservingNewClientRemovesStrayLegacyJarsBeforeFabricCanLoadThem() throws Exception {
        Path game = game(500);
        Path legacy = game.resolve("mods/axialclient-AlphaTest.jar");
        Path versionedLegacy = game.resolve("mods/axial-cosmetics-old.jar");
        Files.write(legacy, mod("1.0.100"));
        Files.write(versionedLegacy, mod("1.0.100"));
        oldLauncher().installBundledClient(game);
        assertFalse(Files.exists(legacy));
        assertFalse(Files.exists(versionedLegacy));
        assertEquals("1.0.500", GitHubReleaseUpdater.installedClientBuild(game));
    }

    private JsonObject release(String tag, boolean draft, String asset) {
        JsonObject release = new JsonObject();
        release.addProperty("tag_name", tag);
        release.addProperty("draft", draft);
        JsonArray assets = new JsonArray();
        JsonObject download = new JsonObject();
        download.addProperty("name", asset);
        download.addProperty("browser_download_url", ASSET.toString());
        assets.add(download);
        release.add("assets", assets);
        return release;
    }

    @Test void shuffledReleasePublicationOrderAlwaysSelectsHighestEligibleBuild() {
        List<JsonObject> releases = List.of(release("1.0.200", false, "app.zip"), release("v1beta.205", false, "app.zip"),
                release("1.0.999", true, "app.zip"), release("1.0.9999", false, "other-os.zip"),
                release("AlphaTest", false, "app.zip"), release("1.0.205-old-999", false, "app.zip"));
        for (int seed = 0; seed < 100; seed++) {
            var shuffled = new ArrayList<>(releases);
            Collections.shuffle(shuffled, new Random(seed));
            JsonArray array = new JsonArray();
            shuffled.forEach(array::add);
            assertEquals("v1beta.205", GitHubReleaseUpdater.newestReleaseWithAsset(array, "app.zip").get("tag_name").getAsString());
        }
    }

    @Test void malformedVersionsCannotMasqueradeAsNewerBuilds() {
        for (String version : List.of("1.0 AlphaTest", "1.0.205-old-999", "1.0.205.b12345", "1.0.205-rc2",
                "../../1.0.999", "1.0.9223372036854775808", "unknown", "", "999")) {
            assertEquals(-1, GitHubReleaseUpdater.buildNumber(version), version);
        }
    }

    private OkHttpClient response(int code, byte[] body) {
        return new OkHttpClient.Builder().addInterceptor(chain -> new Response.Builder()
                .request(chain.request()).protocol(Protocol.HTTP_1_1).code(code).message("test")
                .body(ResponseBody.create(body, null)).build()).build();
    }

    @Test void unavailableOrMalformedReleaseResponsesNeverReportUpToDateOrChangeClient() throws Exception {
        Path game = game(500);
        for (int code : List.of(404, 403, 429, 500)) {
            assertFalse(updater(100, response(code, new byte[0])).checkForUpdate(game).upToDate());
        }
        assertFalse(updater(100, response(200, "{}".getBytes(StandardCharsets.UTF_8))).checkForUpdate(game).upToDate());
        assertFalse(updater(100, response(200, "[]".getBytes(StandardCharsets.UTF_8))).checkForUpdate(game).upToDate());
        assertFalse(updater(100, response(200, "invalid".getBytes(StandardCharsets.UTF_8))).checkForUpdate(game).upToDate());
        assertEquals("1.0.500", GitHubReleaseUpdater.installedClientBuild(game));
    }

    @Test void currentLauncherStillDownloadsWhenActualClientIsOldThroughTheHttpCheck() throws Exception {
        Path game = game(100);
        JsonArray releases = new JsonArray();
        releases.add(release("1.0.500", false, "app.zip"));
        var status = updater(500, response(200, releases.toString().getBytes(StandardCharsets.UTF_8))).checkForUpdate(game);
        assertTrue(status.available());
        assertFalse(status.upToDate());
    }

    @Test void downloadAttemptsHaveSeparateStagingAndCannotOverwriteEachOther() throws Exception {
        Path app = app(500);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry("AxialLauncher/app/client-all.jar"));
            zip.write(Files.readAllBytes(app.resolve("app/client-all.jar")));
            zip.closeEntry();
        }
        var updater = updater(100, response(200, bytes.toByteArray()));
        Path first = updater.downloadAndStage(ASSET, "1.0.500", null);
        Path second = updater.downloadAndStage(ASSET, "1.0.500", null);
        assertNotEquals(first, second);
        GitHubReleaseUpdater.verifyStagedBuild(first, "1.0.500");
        GitHubReleaseUpdater.verifyStagedBuild(second, "1.0.500");
        assertThrows(IOException.class, () -> updater.downloadAndStage(ASSET, "../../1.0.500", null));
    }

    @Test void extractedClientWithWrongBuildIsRejectedBeforeInstalledFilesChange() throws Exception {
        Path game = game(500);
        Path staged = app(501);
        Files.write(staged.resolve("app/client-all.jar"), jar(List.of("version.properties", "axial-cosmetics.jar"),
                List.of("app.version=1.0.501\n".getBytes(StandardCharsets.UTF_8), mod("1.0.100"))));
        byte[] before = Files.readAllBytes(game.resolve("mods/axial-cosmetics.jar"));
        assertThrows(IOException.class, () -> updater(100).installClientUpdate(staged, "1.0.501", game));
        assertArrayEquals(before, Files.readAllBytes(game.resolve("mods/axial-cosmetics.jar")));
    }
}
