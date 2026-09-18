package com.axial.cosmetics.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Fabric-side wrapper for the launcher's embedded Microsoft sign-in helper.
 *
 * MinecraftLauncher supplies the launcher Java executable and launcher
 * classpath through system properties when Minecraft starts.
 */
public final class MicrosoftLoginProcess implements AutoCloseable {

    private static final String CLASSPATH_PROPERTY = "axial.login.classpath";
    private static final String JAVA_PROPERTY = "axial.login.java";
    private static final String LOGIN_MAIN_CLASS =
            "org.example.launcher.EmbeddedMicrosoftLogin";

    private Process process;
    private boolean closed;

    public JsonObject authenticate()
            throws IOException, InterruptedException, TimeoutException {

        String javaExecutable = System.getProperty(JAVA_PROPERTY);
        String launcherClasspath = System.getProperty(CLASSPATH_PROPERTY);

        if (javaExecutable == null || javaExecutable.isBlank()
                || launcherClasspath == null || launcherClasspath.isBlank()) {
            throw new IOException(
                    "Microsoft sign-in environment was not supplied by AxialLauncher"
            );
        }

        Path workDirectory =
                Files.createTempDirectory("axial-microsoft-login-");

        Path result =
                Files.createFile(workDirectory.resolve("result.json"));

        try {
            Process running;

            synchronized (this) {
                if (closed) {
                    throw new CancellationException();
                }

                List<String> command = new ArrayList<>();

                command.add(javaExecutable);
                command.add("--enable-native-access=ALL-UNNAMED");

                if (isMac()) {
                    command.add("--add-opens=java.desktop/sun.awt=ALL-UNNAMED");
                    command.add("--add-opens=java.desktop/sun.lwawt=ALL-UNNAMED");
                    command.add("--add-opens=java.desktop/sun.lwawt.macosx=ALL-UNNAMED");
                }

                command.addAll(List.of(
                        "-cp",
                        launcherClasspath,
                        LOGIN_MAIN_CLASS,
                        result.toAbsolutePath().toString()
                ));

                process = new ProcessBuilder(command)
                        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                        .redirectError(ProcessBuilder.Redirect.DISCARD)
                        .start();

                running = process;
            }

            if (!running.waitFor(6, TimeUnit.MINUTES)) {
                throw new TimeoutException(
                        "Microsoft sign-in timed out"
                );
            }

            synchronized (this) {
                if (closed || running.exitValue() == 2) {
                    throw new CancellationException(
                            "Sign-in cancelled"
                    );
                }
            }

            if (running.exitValue() != 0 || Files.size(result) == 0) {
                throw new IOException(
                        "Microsoft sign-in could not open or complete"
                );
            }

            try {
                return JsonParser
                        .parseString(Files.readString(result))
                        .getAsJsonObject();
            } catch (RuntimeException ex) {
                throw new IOException(
                        "Invalid sign-in response"
                );
            }

        } finally {
            close();

            try (var files = Files.walk(workDirectory)) {
                for (Path file : files
                        .sorted(Comparator.reverseOrder())
                        .toList()) {
                    Files.deleteIfExists(file);
                }
            }
        }
    }

    private static boolean isMac() {
        return System.getProperty("os.name", "")
                .toLowerCase(java.util.Locale.ROOT)
                .contains("mac");
    }

    @Override
    public synchronized void close() {
        closed = true;

        if (process == null || !process.isAlive()) {
            return;
        }

        var descendants = process.descendants().toList();

        descendants.forEach(ProcessHandle::destroy);
        process.destroyForcibly();

        descendants.stream()
                .filter(ProcessHandle::isAlive)
                .forEach(ProcessHandle::destroyForcibly);

        boolean interrupted = false;

        while (process.isAlive()) {
            try {
                process.waitFor();
            } catch (InterruptedException ex) {
                interrupted = true;
            }
        }

        try {
            java.util.concurrent.CompletableFuture
                    .allOf(descendants.stream()
                            .map(ProcessHandle::onExit)
                            .toArray(java.util.concurrent.CompletableFuture[]::new))
                    .get(5, TimeUnit.SECONDS);

        } catch (InterruptedException ex) {
            interrupted = true;

        } catch (java.util.concurrent.ExecutionException
                 | TimeoutException ignored) {
        }

        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}