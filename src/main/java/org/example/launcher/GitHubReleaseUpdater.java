package org.example.launcher;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.Properties;

public final class GitHubReleaseUpdater {
    private static final String API_BASE = "https://api.github.com/repos/";
    private static final Pattern BUILD_VERSION = Pattern.compile("(?:[0-9]+\\.[0-9]+\\.[0-9]+|[0-9]+beta\\.[0-9]+)");
    private final OkHttpClient client;
    private final Path updatesRoot;
    private final AppBuildInfo buildInfo;

    public GitHubReleaseUpdater(AppBuildInfo buildInfo) {
        this(buildInfo, new OkHttpClient(), ClientPaths.appRoot().resolve("updates"));
    }

    GitHubReleaseUpdater(AppBuildInfo buildInfo, OkHttpClient client, Path updatesRoot) {
        this.buildInfo = buildInfo;
        this.client = client;
        this.updatesRoot = updatesRoot;
    }

    public UpdateStatus checkForUpdate(Path gameDir) throws IOException {
        Request request = new Request.Builder()
                .url(API_BASE + buildInfo.githubOwner() + "/" + buildInfo.githubRepo() + "/releases?per_page=100")
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.code() == 404) {
                return UpdateStatus.notAvailable("No GitHub release published yet");
            }
            if (!response.isSuccessful() || response.body() == null) {
                return UpdateStatus.notAvailable("Unable to query latest release");
            }

            JsonElement payload = JsonParser.parseReader(response.body().charStream());
            if (!payload.isJsonArray()) {
                return UpdateStatus.notAvailable("Unable to read published releases");
            }

            JsonObject release = newestReleaseWithAsset(payload.getAsJsonArray(), buildInfo.githubAsset());
            if (release == null) {
                return UpdateStatus.notAvailable("No published release has an update asset");
            }

            String tag = normalizeVersion(release.get("tag_name").getAsString());
            String assetUrl = findAssetUrl(release.getAsJsonArray("assets"), buildInfo.githubAsset());
            if (assetUrl == null) {
                return UpdateStatus.notAvailable("Newest release has no update asset");
            }
            return updateStatusForRelease(tag, URI.create(assetUrl), gameDir);
        }
    }

    UpdateStatus updateStatusForRelease(String tag, URI assetUrl, Path gameDir) throws IOException {
        String installed = installedClientBuild(gameDir);
        if (buildNumber(tag) < 0) {
            return UpdateStatus.notAvailable("Published release has no valid build number");
        }
        long minimum = ClientInstallation.minimumBuild(gameDir);
        if (buildNumber(tag) < minimum) {
            return buildNumber(installed) >= minimum
                    ? UpdateStatus.upToDate(installed)
                    : UpdateStatus.notAvailable("Installed client needs repair; published release is too old");
        }
        if (buildNumber(installed) >= buildNumber(tag)) {
            return UpdateStatus.upToDate(installed);
        }
        if (buildNumber(tag) < buildNumber(buildInfo.appVersion())) {
            return UpdateStatus.notAvailable("Published release is older than this launcher; preparing bundled client");
        }
        // A current launcher does not prove its game payload is current. Download
        // the same release again when the installed client is old or unreadable.
        return UpdateStatus.available(tag, assetUrl);
    }

    void verifyInstallVersion(String releaseTag, Path gameDir) throws IOException {
        if (!updateStatusForRelease(releaseTag, null, gameDir).available()) {
            throw new IOException("Refusing to replace client build " + installedClientBuild(gameDir)
                    + " with " + releaseTag + " (launcher " + buildInfo.appVersion() + ")");
        }
    }

    public Path downloadAndStage(URI downloadUri, String releaseTag, ProgressReporter reporter) throws IOException {
        if (buildNumber(releaseTag) < 0) throw new IOException("Invalid release tag: " + releaseTag);
        Files.createDirectories(updatesRoot);
        Path stagingRoot = Files.createTempDirectory(updatesRoot, "build-" + buildNumber(releaseTag) + "-");
        Path assetFile = stagingRoot.resolve(buildInfo.githubAsset());

        Request request = new Request.Builder()
                .url(downloadUri.toString())
                .header("Accept", "application/octet-stream")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("Failed to download update asset: HTTP " + response.code());
            }
            long contentLength = response.body().contentLength();
            try (InputStream in = response.body().byteStream();
                 OutputStream out = Files.newOutputStream(assetFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                byte[] buffer = new byte[8192];
                long total = 0;
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                    total += read;
                    if (reporter != null && contentLength > 0) {
                        int progress = (int) Math.min(90, (total * 90) / contentLength);
                        reporter.report("Downloading update", progress);
                    }
                }
            }
        }

        Path unpackDir = stagingRoot.resolve("payload");
        deleteRecursive(unpackDir);
        Files.createDirectories(unpackDir);
        unzip(assetFile, unpackDir);
        return locatePackagedApp(unpackDir);
    }

    public void installClientUpdate(Path stagedApp, String releaseTag, Path gameDir) throws IOException {
        if (stagedApp == null || releaseTag == null || releaseTag.isBlank()) {
            throw new IOException("Missing packaged app path for updater");
        }

        ClientInstallation.locked(gameDir, () -> {
            installClientUpdateLocked(stagedApp, releaseTag, gameDir);
            return null;
        });
    }

    private void installClientUpdateLocked(Path stagedApp, String releaseTag, Path gameDir) throws IOException {
        Path modsDir = gameDir.resolve("mods");
        Files.createDirectories(modsDir);

        Path appJar = locateAppJar(stagedApp);
        if (appJar == null) {
            throw new IOException("No packaged client jar found in release app");
        }
        verifyStagedBuild(stagedApp, releaseTag);
        verifyInstallVersion(releaseTag, gameDir);
        ClientInstallation.recordBuild(gameDir, buildNumber(installedClientBuild(gameDir)));

        try (JarFile jarFile = new JarFile(appJar.toFile())) {
            List<String> installedFiles = installPackagedJarsFromBundle(jarFile, modsDir);
            ClientInstallation.recordBuild(gameDir, buildNumber(releaseTag));
            writeInstalledClientManifest(gameDir, installedFiles);
        }

        writeInstalledClientTag(gameDir, releaseTag);
    }

    private static Path locateAppJar(Path appBundle) throws IOException {
        List<Path> appDirs = List.of(
                appBundle.resolve("Contents").resolve("app"),
                appBundle.resolve("app")
        );

        for (Path appDir : appDirs) {
            if (!Files.isDirectory(appDir)) {
                continue;
            }

            try (var stream = Files.list(appDir)) {
                Path jar = stream
                        .filter(p -> p.getFileName() != null && p.getFileName().toString().endsWith(".jar"))
                        .sorted(Comparator
                                .comparing((Path p) -> !p.getFileName().toString().endsWith("-all.jar"))
                                .thenComparing(p -> p.getFileName().toString()))
                        .findFirst()
                        .orElse(null);
                if (jar != null) {
                    return jar;
                }
            }
        }

        return null;
    }

    /**
     * A release asset must have been built by the release identified by its tag.
     * Without this check, a correct, newer GitHub release tag could still carry a
     * stale app zip and overwrite the installed client with an older payload.
     */
    static void verifyStagedBuild(Path stagedApp, String releaseTag) throws IOException {
        long releaseBuild = buildNumber(releaseTag);
        if (releaseBuild < 0) {
            throw new IOException("Update release has no numeric build number: " + releaseTag);
        }

        Path appJar = locateAppJar(stagedApp);
        if (appJar == null) {
            throw new IOException("No packaged client jar found in release app");
        }

        try (JarFile jarFile = new JarFile(appJar.toFile())) {
            var entry = jarFile.getJarEntry("version.properties");
            if (entry == null) {
                throw new IOException("Update app is missing version information");
            }
            try (InputStream in = jarFile.getInputStream(entry)) {
                AppBuildInfo stagedInfo = AppBuildInfo.load(in);
                if (buildNumber(stagedInfo.appVersion()) != releaseBuild) {
                    throw new IOException("Update asset build " + stagedInfo.appVersion()
                            + " does not match release " + releaseTag);
                }
            }
            var clientEntry = jarFile.getJarEntry("axial-cosmetics.jar");
            if (clientEntry == null) {
                throw new IOException("Update app is missing the client payload");
            }
            try (InputStream payload = jarFile.getInputStream(clientEntry)) {
                verifyClientPayload(payload, releaseBuild);
            }
        }
    }

    static void verifyClientPayload(InputStream input, long expectedBuild) throws IOException {
        if (expectedBuild < 0) throw new IOException("Launcher has no valid build number");
        boolean found = false;
        try (JarInputStream payload = new JarInputStream(input)) {
            java.util.jar.JarEntry entry;
            while ((entry = payload.getNextJarEntry()) != null) {
                if (entry.getName().equals("version.properties")) {
                    if (found) throw new IOException("Client payload has duplicate version information");
                    Properties properties = new Properties();
                    properties.load(payload);
                    String version = properties.getProperty("app.version", "");
                    if (buildNumber(version) != expectedBuild) {
                        throw new IOException("Client payload build " + version
                                + " does not match expected build " + expectedBuild);
                    }
                    found = true;
                } else {
                    // Read every entry so corrupt/truncated payload data is rejected too.
                    payload.transferTo(OutputStream.nullOutputStream());
                }
            }
        }
        if (!found) throw new IOException("Client payload is missing version information");
    }

    @FunctionalInterface
    interface FileMover {
        void move(Path source, Path target) throws IOException;
    }

    static List<String> installPackagedJarsFromBundle(JarFile jarFile, Path modsDir) throws IOException {
        return installPackagedJarsFromBundle(jarFile, modsDir, (source, target) -> Files.move(source, target));
    }

    static List<String> installPackagedJarsFromBundle(JarFile jarFile, Path modsDir, FileMover mover) throws IOException {
        Path transaction = Files.createTempDirectory(modsDir.getParent(), ".client-update-");
        Path staged = Files.createDirectory(transaction.resolve("staged"));
        Path backup = Files.createDirectory(transaction.resolve("backup"));
        List<String> installedFiles = new ArrayList<>();
        List<Path> committed = new ArrayList<>();
        try {
            // Finish extracting before touching the working installation.
            var entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                String name = entry.getName();
                if (entry.isDirectory() || !name.endsWith(".jar")
                        || (name.contains("/") && !name.startsWith("launcher-mods/"))) {
                    continue;
                }
                String fileName = Path.of(name).getFileName().toString();
                if (isRuntimeDependencyJar(fileName)) {
                    continue;
                }
                try (InputStream in = jarFile.getInputStream(entry)) {
                    Files.copy(in, staged.resolve(fileName));
                }
                installedFiles.add(fileName);
            }
            if (!installedFiles.contains("axial-cosmetics.jar")) {
                throw new IOException("No client payload found in release bundle");
            }

            try {
                backupManagedMods(modsDir, backup);
                for (String fileName : installedFiles) {
                    Path target = modsDir.resolve(fileName);
                    if (Files.exists(target)) {
                        Files.move(target, backup.resolve(fileName));
                    }
                    mover.move(staged.resolve(fileName), target);
                    committed.add(target);
                }
            } catch (IOException failure) {
                // Restore the previous installation if any replacement fails.
                try {
                    for (Path target : committed) {
                        Files.deleteIfExists(target);
                    }
                    try (var files = Files.list(backup)) {
                        for (Path previous : files.toList()) {
                            Files.move(previous, modsDir.resolve(previous.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                } catch (IOException rollbackFailure) {
                    failure.addSuppressed(rollbackFailure);
                    // Retain the backup for recovery when restoration is impossible.
                    throw new IOException("Update failed; backup retained at " + backup, failure);
                }
                throw failure;
            }
            deleteRecursive(transaction);
            return installedFiles;
        } finally {
            // Never remove a backup that could not be restored.
            try (var files = Files.list(backup)) {
                if (files.findAny().isEmpty()) {
                    deleteRecursive(transaction);
                }
            } catch (java.nio.file.NoSuchFileException ignored) {
                // Successful commit already removed the transaction directory.
            }
        }
    }

    private static void backupManagedMods(Path modsDir, Path backup) throws IOException {
        List<String> previous = readInstalledClientManifest(modsDir.getParent());
        if (!previous.isEmpty()) {
            for (String fileName : previous) {
                Path source = modsDir.resolve(fileName).normalize();
                if (!source.getParent().equals(modsDir.normalize())) {
                    throw new IOException("Invalid managed mod filename: " + fileName);
                }
                if (Files.exists(source)) {
                    Files.move(source, backup.resolve(fileName));
                }
            }
            return;
        }

        if (!Files.isDirectory(modsDir)) {
            return;
        }
        try (var stream = Files.list(modsDir)) {
            for (Path path : stream.toList()) {
                String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
                if (name.endsWith(".jar") && (name.startsWith("axial-") || name.startsWith("axialclient") || name.startsWith("axialutils-")
                        || name.startsWith("geckolib-fabric-") || name.startsWith("fabric-api-")
                        || name.startsWith("fabric-language-kotlin-")
                        || name.startsWith("yet_another_config_lib_v3-") || name.startsWith("zoomify-")
                        || name.startsWith("scrollable-tooltips-")
                        || name.startsWith("sodium-fabric-")
                        || name.startsWith("sodium-extra-") || name.startsWith("reeses-sodium-options-")
                        || name.startsWith("lithium-fabric-") || name.startsWith("ferritecore-")
                        || name.startsWith("immediatelyfast-")
                        || name.startsWith("entityculling-")
                        || name.startsWith("cloth-config-") || name.startsWith("moreculling-")
                        || name.startsWith("staticbgmod-") || name.startsWith("emotecraft-")
                        || name.startsWith("bendablecuboidsfabric-") || name.startsWith("playeranimationlibfabric-")
                        || isRuntimeDependencyJar(name))) {
                    Files.move(path, backup.resolve(path.getFileName()));
                }
            }
        }
    }

    private static boolean isRuntimeDependencyJar(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        return lower.startsWith("javafx-");
    }

    public static String installedClientTag() {
        try {
            Path marker = ClientPaths.clientRoot().resolve(".client-release.tag");
            if (!Files.exists(marker)) {
                return "";
            }
            return Files.readString(marker).trim();
        } catch (IOException ignored) {
            return "";
        }
    }

    /**
     * The release marker is only a record of the last successful updater run. It
     * is not a source of truth: it can remain after a user restores, replaces, or
     * partially installs the mods directory. Read the bundled build information
     * from the actual client JAR so a stale marker can never prevent recovery.
     */
    static String installedClientBuild(Path gameDir) {
        if (gameDir == null) {
            return "";
        }

        Path modJar = gameDir.resolve("mods").resolve("axial-cosmetics.jar");
        if (!Files.isRegularFile(modJar)) {
            return "";
        }

        try (JarFile jarFile = new JarFile(modJar.toFile())) {
            var entry = jarFile.getJarEntry("version.properties");
            if (entry == null) {
                return "";
            }
            try (InputStream in = jarFile.getInputStream(entry)) {
                Properties properties = new Properties();
                properties.load(in);
                String version = properties.getProperty("app.version", "");
                return buildNumber(version) >= 0 ? version : "";
            }
        } catch (IOException | IllegalArgumentException ignored) {
            return "";
        }
    }

    private static void writeInstalledClientTag(Path gameDir, String releaseTag) throws IOException {
        Files.writeString(gameDir.resolve(".client-release.tag"), releaseTag + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
    }

    private static List<String> readInstalledClientManifest(Path gameDir) {
        try {
            Path manifest = gameDir.resolve(".client-release.files");
            if (!Files.exists(manifest)) {
                return List.of();
            }
            return Files.readAllLines(manifest).stream()
                    .map(String::trim)
                    .filter(line -> !line.isBlank())
                    .toList();
        } catch (IOException ignored) {
            return List.of();
        }
    }

    private static void writeInstalledClientManifest(Path gameDir, List<String> fileNames) throws IOException {
        Files.write(gameDir.resolve(".client-release.files"), fileNames, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
    }

    public Path detectCurrentAppBundle() {
        try {
            URI location = AppBuildInfo.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            Path path = Path.of(location).toAbsolutePath().normalize();
            Path current = path;
            while (current != null) {
                if (current.getFileName() != null && current.getFileName().toString().endsWith(".app")) {
                    return current;
                }
                current = current.getParent();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static void unzip(Path zipFile, Path targetDir) throws IOException {
        try (ZipInputStream zin = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                Path out = targetDir.resolve(entry.getName()).normalize();
                if (!out.startsWith(targetDir)) {
                    throw new IOException("Blocked zip entry outside target dir: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(out);
                } else {
                    Files.createDirectories(out.getParent());
                    Files.copy(zin, out, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static Path locatePackagedApp(Path unpackDir) throws IOException {
        try (var stream = Files.walk(unpackDir)) {
            Path macApp = stream
                    .filter(Files::isDirectory)
                    .filter(p -> p.getFileName() != null && p.getFileName().toString().endsWith(".app"))
                    .min(Comparator.comparingInt(Path::getNameCount))
                    .orElse(null);
            if (macApp != null) {
                return macApp;
            }
        }

        try (var stream = Files.walk(unpackDir)) {
            return stream
                    .filter(Files::isDirectory)
                    .filter(p -> Files.isDirectory(p.resolve("app")))
                    .filter(p -> {
                        try {
                            return locateAppJar(p) != null;
                        } catch (IOException ignored) {
                            return false;
                        }
                    })
                    .min(Comparator.comparingInt(Path::getNameCount))
                    .orElseThrow(() -> new IOException("No packaged app found in update asset"));
        }
    }

    private static void deleteRecursive(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (var stream = Files.walk(path)) {
            stream.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                }
            });
        }
    }

    private static String findAssetUrl(JsonArray assets, String preferredName) {
        if (assets == null) return null;
        for (JsonElement element : assets) {
            JsonObject asset = element.getAsJsonObject();
            String name = asset.get("name").getAsString();
            if (name.equalsIgnoreCase(preferredName)) {
                return asset.get("browser_download_url").getAsString();
            }
        }
        return null;
    }

    /**
     * GitHub's {@code /releases/latest} is ordered by publication time. Workflows can
     * finish out of order, so use the greatest release tag instead of allowing a
     * late-published older build to replace the installed client.
     */
    static JsonObject newestReleaseWithAsset(JsonArray releases, String preferredAssetName) {
        JsonObject newest = null;
        for (JsonElement element : releases) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject release = element.getAsJsonObject();
            if (release.has("draft") && release.get("draft").getAsBoolean()) {
                continue;
            }
            if (!release.has("tag_name") || buildNumber(release.get("tag_name").getAsString()) < 0
                    || !release.has("assets")
                    || findAssetUrl(release.getAsJsonArray("assets"), preferredAssetName) == null) {
                continue;
            }
            if (newest == null || isNewer(release.get("tag_name").getAsString(), newest.get("tag_name").getAsString())) {
                newest = release;
            }
        }
        return newest;
    }

    private static String normalizeVersion(String version) {
        if (version == null) return "";
        return version.trim().replaceFirst("^[vV]", "");
    }

    static boolean isNewer(String latest, String current) {
        long latestBuild = buildNumber(latest);
        long currentBuild = buildNumber(current);
        return latestBuild >= 0 && currentBuild >= 0 && latestBuild > currentBuild;
    }

    /**
     * Release tags and packaged app versions share the CI run number as their
     * final numeric component (for example {@code v1beta.245} and
     * {@code 1.0.245}). That number is the only ordering used by the updater.
     */
    static long buildNumber(String version) {
        if (version == null || version.isBlank()) {
            return -1;
        }
        String normalized = normalizeVersion(version);
        if (!BUILD_VERSION.matcher(normalized).matches()) return -1;
        try {
            return Long.parseLong(normalized.substring(normalized.lastIndexOf('.') + 1));
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    public interface ProgressReporter {
        void report(String message, int progress);
    }

    public record UpdateStatus(boolean available, boolean upToDate, String version, URI downloadUri, String message) {
        static UpdateStatus available(String version, URI downloadUri) {
            return new UpdateStatus(true, false, version, downloadUri, "Update available: " + version);
        }

        static UpdateStatus upToDate(String currentVersion) {
            return new UpdateStatus(false, true, currentVersion, null, "Up to date");
        }

        static UpdateStatus notAvailable(String message) {
            return new UpdateStatus(false, false, "", null, message);
        }
    }
}
