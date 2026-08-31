package org.example.launcher;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class StartupController {
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "axial-startup");
        t.setDaemon(false);
        return t;
    });
    private LoadingScreen loadingScreen;
    private final AppBuildInfo buildInfo = AppBuildInfo.load();
    private final GitHubReleaseUpdater updater = new GitHubReleaseUpdater(buildInfo);

    public void start() {
        try {
            SwingUtilities.invokeAndWait(() -> {
                loadingScreen = new LoadingScreen();
                loadingScreen.show();
                loadingScreen.update("Starting", 0);
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw new RuntimeException(e.getCause());
        }
        executor.submit(this::runStartup);
    }

    private void runStartup() {
        try {
            Instant startupShownAt = Instant.now();
            Path gameDir = ClientPaths.clientRoot();
            Files.createDirectories(gameDir);
            if (!Boolean.getBoolean("axial.skipUpdateCheck")) {
                checkForUpdates(gameDir);
            } else {
                loadingScreen.update("Up to date", 10);
            }

            holdStartupScreen(startupShownAt);

            LaunchRequest request = buildLaunchRequest();
            loadingScreen.update("Authenticating", 25);

            MinecraftLauncher launcher = new MinecraftLauncher(msg -> loadingScreen.update(msg, 50));
            loadingScreen.update("Installing client", 55);
            launcher.ensureInstalled(request);

            loadingScreen.update("Launching client", 90);
            Process minecraft = launcher.start(request, System.out);
            closeScreen();
            int exitCode = minecraft.waitFor();
            if (exitCode != 0) {
                throw new IllegalStateException("Minecraft exited with code " + exitCode);
            }
            loadingScreen.update("Up to date", 100);
        } catch (Exception ex) {
            loadingScreen.showError("Launch failed");
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                    null,
                    ex.getMessage(),
                    "Launch failed",
                    JOptionPane.ERROR_MESSAGE));
            closeScreen();
        }
    }

    private void checkForUpdates(Path gameDir) {
        try {
            loadingScreen.update("Checking for updates", 5);
            GitHubReleaseUpdater.UpdateStatus update = updater.checkForUpdate();
            if (update.available()) {
                loadingScreen.update(update.message(), 20);
                loadingScreen.update("Downloading client update", 35);
                Path stagedBundle = updater.downloadAndStage(update.downloadUri(), update.version(), loadingScreen::update);
                loadingScreen.update("Installing client update", 95);
                updater.installClientUpdate(stagedBundle, update.version(), gameDir);
                loadingScreen.update("Client updated", 15);
            } else {
                loadingScreen.update("Up to date", 15);
            }
        } catch (Exception ex) {
            System.err.println("Update check failed; continuing with installed client: " + ex.getMessage());
            loadingScreen.update("Update unavailable", 15);
        }
    }

    private LaunchRequest buildLaunchRequest() throws IOException, InterruptedException, java.util.concurrent.TimeoutException {
        Path gameDir = ClientPaths.clientRoot();
        Files.createDirectories(gameDir);
        Path accountsDir = ClientPaths.accountsDir();
        Files.createDirectories(accountsDir);

        AuthResult authResult = resolveAccount(accountsDir);
        return LaunchRequest.builder()
                .versionId("fabric-1.21.11")
                .gameDir(gameDir)
                .playerName(authResult.playerName)
                .playerUuid(UUID.fromString(authResult.uuid))
                .accessToken(authResult.accessToken)
                .xuid(authResult.xuid)
                .build();
    }

    private AuthResult resolveAccount(Path accountsDir) throws IOException, InterruptedException, java.util.concurrent.TimeoutException {
        if (Files.isDirectory(accountsDir)) {
            Path activeFile = readActiveAccount(accountsDir);
            if (activeFile != null) {
                try {
                    return new AuthManager(activeFile).authenticate();
                } catch (Exception ignored) {
                    // Fall back to other stored accounts if the active one is stale.
                }
            }

            try (var stream = Files.list(accountsDir)) {
                List<Path> files = new ArrayList<>();
                stream.filter(p -> p.getFileName().toString().endsWith(".json"))
                        .sorted()
                        .forEach(files::add);
                for (Path file : files) {
                    try {
                        return new AuthManager(file).authenticate();
                    } catch (Exception ignored) {
                        // Try the next stored account.
                    }
                }
            }
        }

        Path temp = accountsDir.resolve("temp.json");
        AuthManager auth = new AuthManager(temp);
        AuthResult result = auth.authenticate();
        Path target = accountsDir.resolve(result.playerName + ".json");
        Files.deleteIfExists(target);
        Files.move(temp, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        return result;
    }

    private Path readActiveAccount(Path accountsDir) {
        Path pointer = ClientPaths.appRoot().resolve("active-account.path");
        try {
            if (!Files.exists(pointer)) {
                return null;
            }

            String value = Files.readString(pointer).trim();
            if (value.isBlank()) {
                return null;
            }

            Path activeFile = accountsDir.resolve(value);
            return Files.exists(activeFile) ? activeFile : null;
        } catch (IOException ignored) {
            return null;
        }
    }

    private void rebuildLauncher() throws IOException, InterruptedException {
        Path gradlew = Path.of("gradlew").toAbsolutePath().normalize();
        List<String> command = new ArrayList<>();
        command.add(gradlew.toString());
        command.add("classes");
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(Path.of("").toAbsolutePath().normalize().toFile());
        pb.inheritIO();
        Process process = pb.start();
        int exit = process.waitFor();
        if (exit != 0) {
            throw new IOException("Launcher rebuild failed with exit code " + exit);
        }
    }

    private void relaunchUpdatedLauncher() throws IOException {
        List<String> command = new ArrayList<>();
        command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        command.add("-Daxial.skipUpdateCheck=true");
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add("org.example.Main");

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(Path.of("").toAbsolutePath().normalize().toFile());
        pb.inheritIO();
        pb.start();
        closeScreen();
        System.exit(0);
    }

    private void closeScreen() {
        if (loadingScreen != null) {
            loadingScreen.close();
        }
    }

    private void holdStartupScreen(Instant startedAt) {
        Duration minimumVisible = Duration.ofSeconds(10);
        Duration elapsed = Duration.between(startedAt, Instant.now());
        Duration remaining = minimumVisible.minus(elapsed);
        if (!remaining.isPositive()) {
            return;
        }
        loadingScreen.update("Up to date", 15);
        try {
            Thread.sleep(remaining.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
