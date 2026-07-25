package org.example;

import org.example.launcher.ExternalLauncherUI;

public class Main {
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            ExternalLauncherUI ui = new ExternalLauncherUI();
            ui.show();
        });
    }
}
