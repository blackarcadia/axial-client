package org.example;

import org.example.launcher.LauncherInstallationManager;
import org.example.launcher.LauncherStartupMarker;
import org.example.launcher.LoggerUtil;
import org.example.launcher.StartupController;

public class Main {
    public static void main(String[] args) {
        try {
            LoggerUtil.installLauncherLog();
        } catch (Exception e) {
            System.err.println("Launcher logging unavailable: " + e.getMessage());
        }

        try {
            if (LauncherInstallationManager.bootstrapStableInstall()) {
                return;
            }
        } catch (Exception e) {
            System.err.println("Launcher install bootstrap failed: " + e.getMessage());
            e.printStackTrace(System.err);
        }
        LauncherStartupMarker.markStarted();
        new StartupController().start();
    }
}
