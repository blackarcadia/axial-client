package org.example.launcher;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JFrame;
import javax.swing.JProgressBar;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;

public final class LoadingScreen {
    private static final int WINDOW_WIDTH = 960;
    private static final int WINDOW_HEIGHT = 540;
    private final JFrame frame;
    private final JProgressBar progressBar;

    public LoadingScreen() {
        frame = new JFrame("AxialClient");
        frame.setUndecorated(true);
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int x = Math.max(0, (screen.width - WINDOW_WIDTH) / 2);
        int y = Math.max(0, (screen.height - WINDOW_HEIGHT) / 2);
        frame.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        frame.setLocation(x, y);

        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(Color.BLACK);
        JLabel title = new JLabel("AxialClient", JLabel.CENTER);
        title.setForeground(new Color(235, 240, 255));
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        center.add(title, BorderLayout.CENTER);
        frame.add(center, BorderLayout.CENTER);

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setValue(0);
        progressBar.setString("Starting");
        progressBar.setForeground(new Color(50, 190, 255));
        progressBar.setBackground(Color.BLACK);
        progressBar.setBorder(BorderFactory.createEmptyBorder(0, 24, 24, 24));
        frame.add(progressBar, BorderLayout.SOUTH);
    }

    public void show() {
        Runnable showAction = () -> {
            frame.setVisible(true);
        };
        if (SwingUtilities.isEventDispatchThread()) {
            showAction.run();
            return;
        }
        try {
            SwingUtilities.invokeAndWait(showAction);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e.getCause());
        }
    }

    public void update(String message, int progress) {
        SwingUtilities.invokeLater(() -> {
            progressBar.setValue(Math.max(0, Math.min(100, progress)));
            progressBar.setString(message);
        });
    }

    public void close() {
        SwingUtilities.invokeLater(() -> {
            frame.dispose();
        });
    }

    public void showError(String message) {
        update(message, 0);
    }

}
