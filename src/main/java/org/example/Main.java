package org.example;

import org.example.launcher.LauncherInstallationManager;
import org.example.launcher.LauncherStartupMarker;
import org.example.launcher.StartupController;

public class Main {
    public static void main(String[] args) {
        try {
            if (LauncherInstallationManager.bootstrapStableInstall()) {
                return;
            }
        } catch (Exception e) {
            System.err.println("Launcher install bootstrap failed: " + e.getMessage());
        }
        LauncherStartupMarker.markStarted();
        new StartupController().start();
    }
}
