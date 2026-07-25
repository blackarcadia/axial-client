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
import java.net.URL;

public final class LoadingScreen {
    private final JFrame frame;
    private final JProgressBar progressBar;
    private MediaPlayer player;

    public LoadingScreen() {
        frame = new JFrame("AxialClient");
        frame.setUndecorated(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setSize(screen);
        frame.setLocationRelativeTo(null);

        JFXPanel videoPanel = new JFXPanel();
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
        SwingUtilities.invokeLater(() -> frame.setVisible(true));
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
                URL video = LoadingScreen.class.getResource("/loading-background.mp4");
                if (video == null) {
                    videoPanel.setScene(new Scene(new StackPane(), 800, 450));
                    return;
                }

                Media media = new Media(video.toExternalForm());
                player = new MediaPlayer(media);
                player.setCycleCount(MediaPlayer.INDEFINITE);
                player.setMute(true);

                MediaView mediaView = new MediaView(player);
                mediaView.setPreserveRatio(true);

                StackPane root = new StackPane(mediaView);
                root.setStyle("-fx-background-color: black;");
                Scene scene = new Scene(root, frame.getWidth(), frame.getHeight());
                mediaView.fitWidthProperty().bind(root.widthProperty());
                mediaView.fitHeightProperty().bind(root.heightProperty());
                videoPanel.setScene(scene);

                player.setOnReady(player::play);
                player.setOnError(() -> update("Video playback unavailable", 0));
            } catch (MediaException ex) {
                update("Video playback unavailable", 0);
            }
        });
    }
}
