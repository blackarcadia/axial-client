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
    private static final String ACTIVE_INSTALL_MARKER = ".axial-active-install";
    private static final Path LAUNCHER_HOME = ClientPaths.appRoot().resolve("launchers");
    private static final Path ACTIVE_INSTALL_POINTER = ClientPaths.appRoot().resolve("active-launcher.path");

    private LauncherInstallationManager() {
    }

    public static boolean bootstrapStableInstall() throws IOException {
        if (!ClientPaths.isMac()) {
            return false;
        }

        Path currentBundle = detectCurrentAppBundle();
        if (currentBundle == null) {
            return false;
        }

        AppBuildInfo currentInfo = AppBuildInfo.load();
        if (!"release".equalsIgnoreCase(currentInfo.appChannel())) {
            return false;
        }

        if (isActiveInstall(currentBundle)) {
            return false;
        }

        Path activeBundle = activeInstallBundle();
        Path desiredBundle = installPathForVersion(currentInfo.appVersion());

        if (isBundleValid(activeBundle)) {
            AppBuildInfo activeInfo = loadFromBundle(activeBundle);
            if (activeInfo != null) {
                if (!"release".equalsIgnoreCase(currentInfo.appChannel())) {
                    launchBundle(activeBundle);
                    return true;
                }

                if (!isNewer(currentInfo.appVersion(), activeInfo.appVersion())) {
                    launchBundle(activeBundle);
                    return true;
                }
            } else {
                launchBundle(activeBundle);
                return true;
            }
        }

        installBundle(currentBundle, desiredBundle);
        activateInstall(desiredBundle);

        launchBundle(desiredBundle);
        return true;
    }

    public static Path stableAppBundle() {
        Path activeBundle = activeInstallBundle();
        return activeBundle != null ? activeBundle : installPathForVersion(AppBuildInfo.load().appVersion());
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

    public static void installBundle(Path source, Path destination) throws IOException {
        Files.createDirectories(destination.getParent());
        deleteRecursive(destination);
        runCommand("ditto", source.toAbsolutePath().toString(), destination.toAbsolutePath().toString());
        runCommand("xattr", "-dr", "com.apple.quarantine", destination.toAbsolutePath().toString());
        writeActiveMarker(destination);
        resignBundle(destination);
    }

    public static void launchBundle(Path bundle) throws IOException {
        runCommand("open", bundle.toAbsolutePath().toString());
    }

    public static void activateInstall(Path bundle) throws IOException {
        if (bundle == null) {
            throw new IOException("Missing launcher install path");
        }
        Files.createDirectories(ACTIVE_INSTALL_POINTER.getParent());
        Files.writeString(ACTIVE_INSTALL_POINTER, bundle.toAbsolutePath().normalize().toString());
    }

    public static Path activeInstallBundle() {
        try {
            if (!Files.exists(ACTIVE_INSTALL_POINTER)) {
                return null;
            }
            String value = Files.readString(ACTIVE_INSTALL_POINTER).trim();
            if (value.isEmpty()) {
                return null;
            }
            Path bundle = Path.of(value).toAbsolutePath().normalize();
            return Files.exists(bundle) ? bundle : null;
        } catch (IOException ignored) {
            return null;
        }
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

    private static Path installPathForVersion(String version) {
        return LAUNCHER_HOME.resolve(version == null || version.isBlank() ? "0.0.0" : version).resolve("AxialLauncher.app");
    }

    private static boolean isBundleValid(Path bundle) {
        if (bundle == null || !Files.isDirectory(bundle)) {
            return false;
        }
        try {
            return findAppJar(bundle) != null;
        } catch (IOException ignored) {
            return false;
        }
    }

    private static boolean isActiveInstall(Path bundle) {
        if (bundle == null) {
            return false;
        }
        return Files.exists(bundle.resolve("Contents").resolve("Resources").resolve(ACTIVE_INSTALL_MARKER));
    }

    private static void writeActiveMarker(Path bundle) throws IOException {
        Path marker = bundle.resolve("Contents").resolve("Resources").resolve(ACTIVE_INSTALL_MARKER);
        Files.createDirectories(marker.getParent());
        Files.writeString(marker, AppBuildInfo.load().appVersion() + System.lineSeparator());
    }

    public static boolean isNewer(String latest, String current) {
        int latestBuild = lastVersionNumber(latest);
        int currentBuild = lastVersionNumber(current);
        if (latestBuild > 0 || currentBuild > 0) {
            return latestBuild > currentBuild;
        }

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

    private static int lastVersionNumber(String version) {
        if (version == null || version.isBlank()) {
            return 0;
        }

        int current = -1;
        int last = 0;
        for (int i = 0; i < version.length(); i++) {
            char c = version.charAt(i);
            if (c >= '0' && c <= '9') {
                current = Math.max(0, current) * 10 + (c - '0');
            } else if (current >= 0) {
                last = current;
                current = -1;
            }
        }
        return current >= 0 ? current : last;
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
