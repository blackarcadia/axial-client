package com.axial.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class AxialVersion {

    private static final String VERSION = loadVersion();

    private AxialVersion() {
    }

    private static String loadVersion() {
        Properties properties = new Properties();

        try (InputStream input = AxialVersion.class
                .getClassLoader()
                .getResourceAsStream("version.properties")) {

            if (input == null) {
                return "unknown";
            }

            properties.load(input);
            return properties.getProperty("client.version", "unknown");

        } catch (IOException e) {
            return "unknown";
        }
    }

    public static String getVersion() {
        return VERSION;
    }

    public static String getTitle() {
        return "AxialClient " + VERSION;
    }
}