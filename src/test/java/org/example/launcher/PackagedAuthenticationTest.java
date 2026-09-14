package org.example.launcher;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class PackagedAuthenticationTest {
    @TempDir Path directory;

    @Test void packagedModCanCreateAuthenticationClientWithoutLauncherDependencies() throws Throwable {
        Path mod = Path.of(System.getProperty("axial.test.modJar", "build/libs/AxialPrisonsClient-1.0-SNAPSHOT.jar"));
        var urls = new ArrayList<URL>();
        urls.add(mod.toUri().toURL());
        // Gson is supplied by Minecraft. All other authentication dependencies must ship in the mod.
        urls.add(Gson.class.getProtectionDomain().getCodeSource().getLocation());
        try (var jar = new ZipFile(mod.toFile())) {
            for (var entry : jar.stream().filter(e -> e.getName().startsWith("META-INF/jars/") && e.getName().endsWith(".jar")).toList()) {
                Path nested = directory.resolve(Path.of(entry.getName()).getFileName());
                try (var input = jar.getInputStream(entry)) { Files.copy(input, nested); }
                urls.add(nested.toUri().toURL());
            }
        }
        try (var loader = new URLClassLoader(urls.toArray(URL[]::new), ClassLoader.getPlatformClassLoader())) {
            try {
                Class<?> auth = loader.loadClass("net.raphimc.minecraftauth.MinecraftAuth");
                Object client = auth.getMethod("createHttpClient", String.class).invoke(null, "Axial packaging test");
                assertNotNull(client);
                // Resolve the manager's public API as well, without signing in or contacting Microsoft.
                assertNotNull(loader.loadClass("net.raphimc.minecraftauth.java.JavaAuthManager").getDeclaredMethods());
            } catch (InvocationTargetException failure) {
                throw failure.getCause();
            }
        }
    }
}
