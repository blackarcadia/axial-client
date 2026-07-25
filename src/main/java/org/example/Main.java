package org.example;

import org.example.launcher.StartupController;

public class Main {
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> new StartupController().start());
    }
}
