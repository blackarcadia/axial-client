package org.example;

import org.example.launcher.LauncherInstallationManager;
import org.example.launcher.LauncherStartupMarker;
import org.example.launcher.AccountLoginController;
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

        if (hasArg(args, "--add-account")) {
            new AccountLoginController().start();
            return;
        }

        LauncherStartupMarker.markStarted();
        new StartupController().start();
    }

    private static boolean hasArg(String[] args, String target) {
        if (args == null) {
            return false;
        }
        for (String arg : args) {
            if (target.equals(arg)) {
                return true;
            }
        }
        return false;
    }
}
