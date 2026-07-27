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
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.jar.JarFile;

public final class GitHubReleaseUpdater {
    private static final String API_BASE = "https://api.github.com/repos/";
    private static final Pattern VERSION_SPLIT = Pattern.compile("[^0-9]+");
    private static final Path CLIENT_RELEASE_MARKER = Path.of(
            System.getProperty("user.home"),
            "Library",
            "Application Support",
            "AxialLauncher",
            "client-release.tag"
    );
    private static final String AXIAL_COSMETICS_ENTRY = "axial-cosmetics.jar";
    private static final String AXIAL_UTILS_ENTRY = "axialutils-1.0-SNAPSHOT.jar";
    private static final String GECKOLIB_ENTRY = "geckolib-fabric-1.21.11-5.4.5.jar";
    private static final String SODIUM_ENTRY = "sodium-fabric-0.8.13+mc1.21.11.jar";
    private static final String LITHIUM_ENTRY = "lithium-fabric-0.21.4+mc1.21.11.jar";
    private static final String STATIC_BG_ENTRY = "staticbgmod-1.0.4.jar";

    private final OkHttpClient client = new OkHttpClient();
    private final AppBuildInfo buildInfo;

    public GitHubReleaseUpdater(AppBuildInfo buildInfo) {
        this.buildInfo = buildInfo;
    }

    public UpdateStatus checkForUpdate() throws IOException {
        Request request = new Request.Builder()
                .url(API_BASE + buildInfo.githubOwner() + "/" + buildInfo.githubRepo() + "/releases/latest")
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

            JsonObject release = JsonParser.parseReader(response.body().charStream()).getAsJsonObject();
            String tag = normalizeVersion(release.get("tag_name").getAsString());
            String installedTag = installedClientTag();
            if (!installedTag.isBlank() && installedTag.equalsIgnoreCase(tag)) {
                return UpdateStatus.upToDate(installedTag);
            }

            String assetUrl = findAssetUrl(release.getAsJsonArray("assets"), buildInfo.githubAsset());
            if (assetUrl == null) {
                return UpdateStatus.notAvailable("Latest release has no update asset");
            }
            return UpdateStatus.available(tag, URI.create(assetUrl));
        }
    }

    public Path downloadAndStage(URI downloadUri, String releaseTag, ProgressReporter reporter) throws IOException {
        Path stagingRoot = Path.of(System.getProperty("user.home"), "Library", "Application Support", "AxialLauncher", "updates", releaseTag);
        Files.createDirectories(stagingRoot);
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
        return locateAppBundle(unpackDir);
    }

    public void installClientUpdate(Path stagedAppBundle, String releaseTag, Path gameDir) throws IOException {
        if (stagedAppBundle == null || releaseTag == null || releaseTag.isBlank()) {
            throw new IOException("Missing app bundle path for updater");
        }

        Path modsDir = gameDir.resolve("mods");
        Files.createDirectories(modsDir);

        Path appJar = locateAppJar(stagedAppBundle);
        if (appJar == null) {
            throw new IOException("No packaged client jar found in release bundle");
        }

        try (JarFile jarFile = new JarFile(appJar.toFile())) {
            requireExactJarFromBundle(jarFile, modsDir, AXIAL_COSMETICS_ENTRY, "axial-cosmetics", "axial-cosmetics-");
            requireExactJarFromBundle(jarFile, modsDir, AXIAL_UTILS_ENTRY, "axialutils", "axialutils-");
            requireExactJarFromBundle(jarFile, modsDir, GECKOLIB_ENTRY, "geckolib-fabric", "geckolib-fabric-");
            requireExactJarFromBundle(jarFile, modsDir, SODIUM_ENTRY, "sodium-fabric", "sodium-fabric-", "sodium-extra-", "reeses-sodium-options-");
            requireExactJarFromBundle(jarFile, modsDir, LITHIUM_ENTRY, "lithium-fabric", "lithium-fabric-");
            requireExactJarFromBundle(jarFile, modsDir, STATIC_BG_ENTRY, "staticbgmod", "staticbgmod-");
        }

        writeInstalledClientTag(releaseTag);
    }

    private static Path locateAppJar(Path appBundle) throws IOException {
        Path appDir = appBundle.resolve("Contents").resolve("app");
        if (!Files.isDirectory(appDir)) {
            return null;
        }

        try (var stream = Files.list(appDir)) {
            return stream
                    .filter(p -> p.getFileName() != null && p.getFileName().toString().endsWith(".jar"))
                    .min(Comparator.comparingInt(Path::getNameCount))
                    .orElse(null);
        }
    }

    private static void requireExactJarFromBundle(JarFile jarFile, Path modsDir, String entryName, String targetPrefix, String... deletePrefixes) throws IOException {
        deleteMatchingMods(modsDir, targetPrefix, deletePrefixes);
        var jarEntry = jarFile.getJarEntry(entryName);
        if (jarEntry == null) {
            throw new IOException("Expected packaged mod jar missing from release bundle: " + entryName);
        }

        Path target = modsDir.resolve(entryName);
        try (InputStream in = jarFile.getInputStream(jarEntry);
             OutputStream out = Files.newOutputStream(target, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            in.transferTo(out);
        }
    }

    private static void deleteMatchingMods(Path modsDir, String prefix, String... deletePrefixes) throws IOException {
        if (!Files.isDirectory(modsDir)) {
            return;
        }
        try (var stream = Files.list(modsDir)) {
            for (Path path : stream.toList()) {
                String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
                if (name.equals(prefix + ".jar") || Arrays.stream(deletePrefixes).anyMatch(name::startsWith)) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    public static String installedClientTag() {
        try {
            if (!Files.exists(CLIENT_RELEASE_MARKER)) {
                return "";
            }
            return Files.readString(CLIENT_RELEASE_MARKER).trim();
        } catch (IOException ignored) {
            return "";
        }
    }

    private static void writeInstalledClientTag(String releaseTag) throws IOException {
        Files.createDirectories(CLIENT_RELEASE_MARKER.getParent());
        Files.writeString(CLIENT_RELEASE_MARKER, releaseTag + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
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

    private static Path locateAppBundle(Path unpackDir) throws IOException {
        try (var stream = Files.walk(unpackDir)) {
            return stream
                    .filter(p -> p.getFileName() != null && p.getFileName().toString().endsWith(".app"))
                    .min(Comparator.comparingInt(Path::getNameCount))
                    .orElseThrow(() -> new IOException("No .app bundle found in update asset"));
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
        for (JsonElement element : assets) {
            JsonObject asset = element.getAsJsonObject();
            String name = asset.get("name").getAsString().toLowerCase(Locale.ROOT);
            if (name.endsWith(".zip") && name.contains("axial")) {
                return asset.get("browser_download_url").getAsString();
            }
        }
        return null;
    }

    private static String normalizeVersion(String version) {
        if (version == null) return "";
        return version.trim().replaceFirst("^[vV]", "");
    }

    private static boolean isNewer(String latest, String current) {
        int[] left = parseVersion(latest);
        int[] right = parseVersion(current);
        int len = Math.max(left.length, right.length);
        for (int i = 0; i < len; i++) {
            int a = i < left.length ? left[i] : 0;
            int b = i < right.length ? right[i] : 0;
            if (a != b) {
                return a > b;
            }
        }
        return false;
    }

    private static int[] parseVersion(String version) {
        String normalized = normalizeVersion(version);
        String[] parts = VERSION_SPLIT.split(normalized);
        List<Integer> values = new ArrayList<>();
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            try {
                values.add(Integer.parseInt(part));
            } catch (NumberFormatException ignored) {
            }
        }
        if (values.isEmpty()) {
            return new int[]{0};
        }
        return values.stream().mapToInt(Integer::intValue).toArray();
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
