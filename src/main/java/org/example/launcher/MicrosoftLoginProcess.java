package org.example.launcher;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/** Exchanges a login result through a private temporary file; credentials never enter command lines. */
public final class MicrosoftLoginProcess implements AutoCloseable {
    public static final String CLASSPATH_PROPERTY = "axial.login.classpath";
    public static final String JAVA_PROPERTY = "axial.login.java";
    private Process process;
    private boolean closed;

    public JsonObject authenticate() throws IOException, InterruptedException, TimeoutException {
        Path result = Files.createTempFile("axial-microsoft-login-", ".json");
        try {
            Process running;
            synchronized (this) {
                if (closed) throw new CancellationException();
                process = new ProcessBuilder(command(result))
                        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                        .redirectError(ProcessBuilder.Redirect.DISCARD).start();
                running = process;
            }
            if (!running.waitFor(6, TimeUnit.MINUTES)) throw new TimeoutException("Microsoft sign-in timed out");
            synchronized (this) {
                if (closed || running.exitValue() == 2) throw new CancellationException("Sign-in cancelled");
            }
            if (running.exitValue() != 0 || Files.size(result) == 0) {
                throw new IOException("Microsoft sign-in could not open or complete. Please update the Axial launcher and retry.");
            }
            try { return JsonParser.parseString(Files.readString(result)).getAsJsonObject(); }
            catch (RuntimeException ex) { throw new IOException("Invalid sign-in response"); }
        } finally {
            close();
            Files.deleteIfExists(result);
        }
    }

    static List<String> command(Path result) {
        return List.of(System.getProperty(JAVA_PROPERTY, javaExecutable()), "-cp",
                System.getProperty(CLASSPATH_PROPERTY, launcherClasspath()),
                EmbeddedMicrosoftLogin.class.getName(), result.toAbsolutePath().toString());
    }

    public static String javaExecutable() {
        return Path.of(System.getProperty("java.home"), "bin", ClientPaths.isWindows() ? "java.exe" : "java").toString();
    }

    public static String launcherClasspath() {
        // Minecraft runs from a different working directory than the launcher.
        return Arrays.stream(System.getProperty("java.class.path").split(java.util.regex.Pattern.quote(File.pathSeparator)))
                .map(entry -> Path.of(entry).toAbsolutePath().normalize().toString())
                .collect(Collectors.joining(File.pathSeparator));
    }

    @Override public synchronized void close() {
        closed = true;
        if (process != null && process.isAlive()) {
            process.destroyForcibly();
            boolean interrupted = false;
            while (process.isAlive()) {
                try { process.waitFor(); }
                catch (InterruptedException ex) { interrupted = true; }
            }
            if (interrupted) Thread.currentThread().interrupt();
        }
    }
}
