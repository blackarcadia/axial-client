package org.example.launcher;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.net.URL;

public final class LoadingScreen {
    private static final int WINDOW_WIDTH = 960;
    private static final int WINDOW_HEIGHT = 540;
    private final JFrame frame;
    private final JProgressBar progressBar;
    private final JFXPanel videoPanel;
    private MediaPlayer player;
    private Path tempVideoFile;

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

        videoPanel = new JFXPanel();
        videoPanel.setBackground(Color.BLACK);
        frame.add(videoPanel, BorderLayout.CENTER);

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setValue(0);
        progressBar.setString("Starting");
        progressBar.setForeground(new Color(50, 190, 255));
        progressBar.setBackground(Color.BLACK);
        progressBar.setBorder(BorderFactory.createEmptyBorder(0, 24, 24, 24));
        frame.add(progressBar, BorderLayout.SOUTH);

        initVideo(videoPanel);
    }

    public void show() {
        Runnable showAction = () -> {
            frame.setVisible(true);
            frame.toFront();
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
            if (player != null) {
                try {
                    player.stop();
                    player.dispose();
                } catch (Exception ignored) {
                }
            }
            frame.dispose();
        });
    }

    public void showError(String message) {
        update(message, 0);
    }

    private void initVideo(JFXPanel videoPanel) {
        Platform.runLater(() -> {
            try {
                Platform.setImplicitExit(false);
                if (LoadingScreen.class.getResource("/loading-background.mp4") == null) {
                    videoPanel.setScene(new Scene(new StackPane(), 800, 450));
                    return;
                }

                Path source = extractVideoToTemp();
                Media media = new Media(source.toUri().toString());
                player = new MediaPlayer(media);
                player.setCycleCount(MediaPlayer.INDEFINITE);
                player.setMute(true);
                player.setAutoPlay(true);
                player.setOnError(() -> update("Video playback unavailable", 0));
                media.setOnError(() -> update("Video playback unavailable", 0));
                media.widthProperty().addListener((obs, oldValue, newValue) -> update("Starting", progressBar.getValue()));

                MediaView mediaView = new MediaView(player);
                mediaView.setPreserveRatio(true);
                mediaView.setSmooth(true);
                mediaView.setOnError(e -> update("Video playback unavailable", 0));

                StackPane root = new StackPane(mediaView);
                root.setStyle("-fx-background-color: black;");
                Scene scene = new Scene(root, frame.getWidth(), Math.max(1, frame.getHeight() - 120));
                mediaView.fitWidthProperty().bind(root.widthProperty());
                mediaView.fitHeightProperty().bind(root.heightProperty());
                videoPanel.setScene(scene);

                player.statusProperty().addListener((obs, oldStatus, newStatus) -> {
                    if (newStatus == MediaPlayer.Status.READY) {
                        update("Starting", progressBar.getValue());
                        player.play();
                    }
                });
                player.setOnReady(() -> {
                    update("Starting", progressBar.getValue());
                    player.play();
                });
            } catch (MediaException ex) {
                showFallback();
                update("Video playback unavailable", 0);
            } catch (Exception ex) {
                showFallback();
                update("Video playback unavailable", 0);
            }
        });
    }

    private void showFallback() {
        Platform.runLater(() -> {
            StackPane root = new StackPane();
            root.setStyle("-fx-background-color: black;");
            videoPanel.setScene(new Scene(root, frame.getWidth(), Math.max(1, frame.getHeight() - 120)));
        });
    }

    private Path extractVideoToTemp() throws IOException {
        if (tempVideoFile != null && Files.exists(tempVideoFile)) {
            return tempVideoFile;
        }
        tempVideoFile = Files.createTempFile("axial-loading-", ".mp4");
        tempVideoFile.toFile().deleteOnExit();
        try (InputStream in = LoadingScreen.class.getResourceAsStream("/loading-background.mp4")) {
            if (in == null) {
                throw new IOException("loading-background.mp4 missing from resources");
            }
            Files.copy(in, tempVideoFile, StandardCopyOption.REPLACE_EXISTING);
        }
        return tempVideoFile;
    }
}
