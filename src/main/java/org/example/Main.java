package org.example;

import org.example.launcher.LauncherStartupMarker;
import org.example.launcher.StartupController;

public class Main {
    public static void main(String[] args) {
        LauncherStartupMarker.markStarted();
        new StartupController().start();
    }
}
