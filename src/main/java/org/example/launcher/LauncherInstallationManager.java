package org.example.launcher;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

public final class LauncherInstallationManager {
    private static final Pattern VERSION_SPLIT = Pattern.compile("[^0-9]+");
    private static final Path STABLE_APP_BUNDLE = Path.of(
            System.getProperty("user.home"),
            "Library",
            "Application Support",
            "AxialLauncher",
            "AxialLauncher.app"
    );

    private LauncherInstallationManager() {
    }

    public static boolean bootstrapStableInstall() throws IOException {
        Path currentBundle = detectCurrentAppBundle();
        if (currentBundle == null) {
            return false;
        }

        if (currentBundle.equals(STABLE_APP_BUNDLE)) {
            return false;
        }

        AppBuildInfo currentInfo = AppBuildInfo.load();
        AppBuildInfo stableInfo = Files.exists(STABLE_APP_BUNDLE) ? loadFromBundle(STABLE_APP_BUNDLE) : null;

        if (stableInfo == null || isNewer(currentInfo.appVersion(), stableInfo.appVersion())) {
            installBundle(currentBundle, STABLE_APP_BUNDLE);
        }

        launchBundle(STABLE_APP_BUNDLE);
        return true;
    }

    public static Path stableAppBundle() {
        return STABLE_APP_BUNDLE;
    }

    public static Path detectCurrentAppBundle() {
        try {
            var location = AppBuildInfo.class.getProtectionDomain().getCodeSource().getLocation().toURI();
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

    public static AppBuildInfo loadFromBundle(Path appBundle) throws IOException {
        Path jar = findAppJar(appBundle);
        if (jar == null) {
            return new AppBuildInfo("0.0.0", "dev", "blackarcadia", "axial-client", "AxialLauncher.app.zip");
        }

        try (JarFile jarFile = new JarFile(jar.toFile())) {
            try (InputStream in = jarFile.getInputStream(jarFile.getJarEntry("version.properties"))) {
                if (in == null) {
                    return new AppBuildInfo("0.0.0", "dev", "blackarcadia", "axial-client", "AxialLauncher.app.zip");
                }
                return AppBuildInfo.load(in);
            }
        }
    }

    private static void installBundle(Path source, Path destination) throws IOException {
        Files.createDirectories(destination.getParent());
        deleteRecursive(destination);
        runCommand("ditto", source.toAbsolutePath().toString(), destination.toAbsolutePath().toString());
        runCommand("xattr", "-dr", "com.apple.quarantine", destination.toAbsolutePath().toString());
        resignBundle(destination);
    }

    private static void launchBundle(Path bundle) throws IOException {
        runCommand("open", bundle.toAbsolutePath().toString());
    }

    private static Path findAppJar(Path appBundle) throws IOException {
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

    private static void runCommand(String... command) throws IOException {
        try {
            Process process = new ProcessBuilder(command).start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("Command failed: " + String.join(" ", command));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while running: " + String.join(" ", command), e);
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

    private static void resignBundle(Path bundle) throws IOException {
        try {
            runCommand("codesign", "--force", "--deep", "--sign", "-", bundle.toAbsolutePath().toString());
        } catch (IOException ignored) {
            // If ad-hoc signing is unavailable, keep the bundle rather than failing the install.
        }
    }

    public static boolean isNewer(String latest, String current) {
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
        if (version == null) {
            return new int[] {0};
        }

        String[] parts = VERSION_SPLIT.split(version.trim().replaceFirst("^[vV]", ""));
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
            return new int[] {0};
        }
        return values.stream().mapToInt(Integer::intValue).toArray();
    }
}
