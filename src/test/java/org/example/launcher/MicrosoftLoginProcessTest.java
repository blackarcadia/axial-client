package org.example.launcher;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.concurrent.CancellationException;

import static org.junit.jupiter.api.Assertions.*;

class MicrosoftLoginProcessTest {
    @TempDir Path directory;

    @Test void keepsPathsWithSpacesAsSingleProcessArguments() {
        String previousJava = System.getProperty(MicrosoftLoginProcess.JAVA_PROPERTY);
        String previousClasspath = System.getProperty(MicrosoftLoginProcess.CLASSPATH_PROPERTY);
        try {
            System.setProperty(MicrosoftLoginProcess.JAVA_PROPERTY, "/Applications/Axial Client/runtime/bin/java");
            System.setProperty(MicrosoftLoginProcess.CLASSPATH_PROPERTY, "/Applications/Axial Client/app/launcher.jar");
            Path result = directory.resolve("login result.json");
            var command = MicrosoftLoginProcess.command(result);
            assertEquals(5, command.size());
            assertEquals("/Applications/Axial Client/runtime/bin/java", command.get(0));
            assertEquals("/Applications/Axial Client/app/launcher.jar", command.get(2));
            assertEquals("org.example.launcher.EmbeddedMicrosoftLogin", command.get(3));
            assertEquals(result.toString(), command.get(4));
        } finally {
            restore(MicrosoftLoginProcess.JAVA_PROPERTY, previousJava);
            restore(MicrosoftLoginProcess.CLASSPATH_PROPERTY, previousClasspath);
        }
    }

    @Test void cancelledLoginCannotStartAnotherWindow() {
        var login = new MicrosoftLoginProcess();
        login.close();
        assertThrows(CancellationException.class, login::authenticate);
        assertDoesNotThrow(login::close);
    }

    private static void restore(String key, String value) {
        if (value == null) System.clearProperty(key);
        else System.setProperty(key, value);
    }
}
