package org.example.launcher;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public final class LoggerUtil {
    private LoggerUtil() {}

    public static PrintStream teeToConsoleWindowAndFile(PrintStream console, PrintStream window, Path logFile) throws IOException {
        Files.createDirectories(logFile.getParent());
        OutputStream fos = Files.newOutputStream(logFile);
        PrintStream filePs = new PrintStream(fos, true);
        TeeOutputStream tee = new TeeOutputStream(new TeeOutputStream(console, window), filePs);
        PrintStream ps = new PrintStream(tee, true);
        ps.println("==== AxialLauncher start " + ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) + " ====");
        return ps;
    }
}
