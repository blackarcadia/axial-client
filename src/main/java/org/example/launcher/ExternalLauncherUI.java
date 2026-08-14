package org.example.launcher;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExternalLauncherUI {
    private final JFrame frame;
    private final JButton launchButton;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private PrintStream logStream;
    private Image backgroundImage;
    private final BackgroundPanel panel;
    private final FadeOverlay openingFadeOverlay = new FadeOverlay();
    private final JComboBox<AccountEntry> accountSelect;
    private final JButton loginButton;
    private final JButton logoutButton;
    private final Path accountsDir = ClientPaths.accountsDir();
    private final JLabel activeAccountLabel;
    private final JLabel headLabel;
    private final JLabel switchLabel;
    private Image switchBaseImage;
    private final int switchW = 200;
    private final int switchH = 100;

    public ExternalLauncherUI() {
        frame = new JFrame("AxialClient Version 1.0 Alphatest");
        setFrameIcon(frame);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setResizable(false);

        try {
            backgroundImage = javax.imageio.ImageIO.read(ExternalLauncherUI.class.getResource("/launch-bg.png"));

            Path logsDir = Path.of(System.getProperty("user.home"), "Library", "Logs", "AxialLauncher");
            Files.createDirectories(logsDir);
            Path logFile = logsDir.resolve("latest.log");
            PrintStream fileStream = new PrintStream(Files.newOutputStream(logFile), true);
            logStream = new PrintStream(new TeeOutputStream(System.out, fileStream), true);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Unable to open log file: " + e.getMessage(), "Logging error", JOptionPane.ERROR_MESSAGE);
        }

        double scale = 0.7;
        int targetWidth = (int) (backgroundImage.getWidth(null) * scale);
        int targetHeight = (int) (backgroundImage.getHeight(null) * scale);
        panel = new BackgroundPanel(backgroundImage, targetWidth, targetHeight);
        panel.setLayout(null);

        launchButton = new JButton();
        launchButton.setOpaque(false);
        launchButton.setContentAreaFilled(false);
        launchButton.setBorderPainted(false);
        launchButton.setFocusPainted(false);
        launchButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        launchButton.addActionListener(e -> launch());

        int buttonWidth = targetWidth / 2;
        int buttonHeight = 200;
        int x = (targetWidth - buttonWidth) / 2;
        int y = 0;
        launchButton.setBounds(x, y, buttonWidth, buttonHeight);
        panel.add(launchButton);

        accountSelect = new JComboBox<>();
        loginButton = new JButton();
        logoutButton = new JButton("Logout");
        activeAccountLabel = new JLabel("");
        headLabel = new JLabel();
        switchLabel = new JLabel();
        loginButton.addActionListener(e -> loginNewAccount());
        logoutButton.addActionListener(e -> logoutSelected());
        try {
            Image img = javax.imageio.ImageIO.read(ExternalLauncherUI.class.getResource("/login-btn.png"));
            Image scaled = img.getScaledInstance(180, 120, Image.SCALE_SMOOTH);
            loginButton.setIcon(new ImageIcon(scaled));
            loginButton.setBorderPainted(false);
            loginButton.setContentAreaFilled(false);
            loginButton.setFocusPainted(false);
            loginButton.setOpaque(false);
        } catch (Exception ignored) {
        }
        JComboBox<AccountEntry> box = accountSelect;
        try {
            Font futuristic = Font.createFont(Font.TRUETYPE_FONT, new java.io.File("/System/Library/Fonts/Supplemental/Futura.ttc")).deriveFont(Font.PLAIN, 14f);
            box.setFont(futuristic);
            activeAccountLabel.setFont(futuristic);
            logoutButton.setFont(futuristic);
        } catch (Exception ignored) {}

        int barWidth = 360;
        int barHeight = 120;
        JPanel accountPanel = new JPanel();
        accountPanel.setOpaque(false);
        accountPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 5,5));
        activeAccountLabel.setForeground(Color.WHITE);
        headLabel.setPreferredSize(new Dimension(48,48));
        try {
            switchBaseImage = javax.imageio.ImageIO.read(ExternalLauncherUI.class.getResource("/account-switch.png"));
            Image scaledSwitch = switchBaseImage.getScaledInstance(switchW, switchH, Image.SCALE_SMOOTH);
            switchLabel.setIcon(new ImageIcon(scaledSwitch));
            accountPanel.add(switchLabel);
        } catch (Exception ignored) {}
        accountPanel.add(accountSelect);
        accountPanel.add(headLabel);
        accountPanel.add(activeAccountLabel);
        accountPanel.add(loginButton);
        accountPanel.add(logoutButton);
        accountPanel.setBounds(targetWidth - barWidth - 10, 10, barWidth, barHeight);
        panel.add(accountPanel);

        refreshAccounts();

        frame.setContentPane(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
    }

    private void setFrameIcon(JFrame frame) {
        try {
            Image icon = javax.imageio.ImageIO.read(ExternalLauncherUI.class.getResource("/app-icon.png"));
            frame.setIconImage(icon);
        } catch (Exception ignored) {
        }
    }

    public void show() {
        openingFadeOverlay.setAlpha(0.34f);
        frame.setGlassPane(openingFadeOverlay);
        openingFadeOverlay.setVisible(true);
        frame.setVisible(true);
        animateMenuOpen();
        log("Ready. Click launch to start Minecraft.");
    }

    private void animateMenuOpen() {
        Timer timer = new Timer(1000 / 60, null);
        long startedAt = System.nanoTime();
        int holdMs = 35;
        int durationMs = 180;
        timer.addActionListener(e -> {
            float elapsedMs = (System.nanoTime() - startedAt) / 1_000_000f;
            float progress = Math.min(1f, Math.max(0f, (elapsedMs - holdMs) / durationMs));
            float eased = 1f - (float) Math.pow(1f - progress, 3);
            openingFadeOverlay.setAlpha(0.34f * (1f - eased));
            if (progress >= 1f) {
                openingFadeOverlay.setVisible(false);
                timer.stop();
            }
        });
        timer.start();
    }

    private void launch() {
        launchButton.setEnabled(false);
        log("Starting authentication and install...");
        executor.submit(() -> {
            try {
                AccountEntry selected = (AccountEntry) accountSelect.getSelectedItem();
                if (selected == null) throw new IllegalStateException("No account selected");

                PrintStream logTee = logStream != null ? logStream : System.out;

                Path gameDir = ClientPaths.clientRoot();
                Files.createDirectories(gameDir);
                Path store = accountsDir.resolve(selected.fileName);
                AuthManager auth = new AuthManager(store);
                AuthResult authResult = auth.authenticate();

                LaunchRequest request = LaunchRequest.builder()
                        .versionId("fabric-1.21.11")
                        .gameDir(gameDir)
                        .playerName(authResult.playerName)
                        .playerUuid(UUID.fromString(authResult.uuid))
                        .accessToken(authResult.accessToken)
                        .xuid(authResult.xuid)
                        .build();

                MinecraftLauncher launcher = new MinecraftLauncher(msg -> logTee.println(msg));
                launcher.ensureInstalled(request);
                log("Launching game...");
                launcher.launch(request, logTee);
                log("Game exited normally.");
            } catch (Exception ex) {
                log("Error: " + ex.getMessage());
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(frame, ex.getMessage(), "Launch failed", JOptionPane.ERROR_MESSAGE));
            } finally {
                SwingUtilities.invokeLater(() -> launchButton.setEnabled(true));
            }
        });
    }

    private void log(String text) {
        if (logStream != null) {
            logStream.println(text);
        } else {
            System.out.println(text);
        }
    }

    private void loginNewAccount() {
        executor.submit(() -> {
            try {
                Files.createDirectories(accountsDir);
                Path temp = accountsDir.resolve("temp.json");
                AuthManager auth = new AuthManager(temp);
                AuthResult result = auth.authenticate();
                Path target = accountsDir.resolve(result.playerName + ".json");
                Files.deleteIfExists(target);
                Files.move(temp, target);
                log("Logged in as " + result.playerName);
                refreshAccounts();
                SwingUtilities.invokeLater(() -> accountSelect.setSelectedItem(new AccountEntry(result.playerName, result.uuid, target.getFileName().toString())));
            } catch (Exception ex) {
                log("Login failed: " + ex);
                ex.printStackTrace(logStream != null ? logStream : System.out);
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(frame, ex.getMessage(), "Login failed", JOptionPane.ERROR_MESSAGE));
                SwingUtilities.invokeLater(this::updateActiveDisplay);
            } finally {
                SwingUtilities.invokeLater(() -> {
                    loginButton.setVisible(true);
                    logoutButton.setVisible(false);
                });
            }
        });
    }

    private void logoutSelected() {
        AccountEntry selected = (AccountEntry) accountSelect.getSelectedItem();
        if (selected == null) return;
        Path path = accountsDir.resolve(selected.fileName);
        try {
            Files.deleteIfExists(path);
            log("Removed account " + selected.displayName);
            refreshAccounts();
            SwingUtilities.invokeLater(() -> updateActiveDisplay());
        } catch (IOException e) {
            log("Failed to remove account: " + e.getMessage());
        }
    }

    private void refreshAccounts() {
        SwingUtilities.invokeLater(() -> accountSelect.removeAllItems());
        List<AccountEntry> entries = new ArrayList<>();
        if (Files.isDirectory(accountsDir)) {
            try (var stream = Files.list(accountsDir)) {
                stream.filter(p -> p.toString().endsWith(".json")).forEach(p -> {
                    try {
                        AuthResult peek = AuthManager.peek(p);
                        if (peek != null) {
                            entries.add(new AccountEntry(peek.playerName, peek.uuid, p.getFileName().toString()));
                        }
                    } catch (IOException ignored) {
                    }
                });
            } catch (IOException ignored) {
            }
        }
        if (entries.isEmpty()) {
            entries.add(new AccountEntry("Click Login", "", "default.json"));
        }
        SwingUtilities.invokeLater(() -> {
            for (AccountEntry e : entries) accountSelect.addItem(e);
            accountSelect.setSelectedIndex(0);
            updateActiveDisplay();
        });
    }

    private void updateActiveDisplay() {
        AccountEntry sel = (AccountEntry) accountSelect.getSelectedItem();
        if (sel != null && sel.uuid != null && !sel.uuid.isBlank()) {
            setHeadAsync(sel.uuid);
            loginButton.setVisible(false);
            logoutButton.setVisible(true);
            accountSelect.setVisible(true);
            headLabel.setVisible(true);
            activeAccountLabel.setVisible(false);
            switchLabel.setVisible(true);
        } else {
            headLabel.setIcon(null);
            loginButton.setVisible(true);
            logoutButton.setVisible(false);
            accountSelect.setVisible(false);
            headLabel.setVisible(false);
            activeAccountLabel.setVisible(false);
            if (switchBaseImage != null) {
                Image scaledSwitch = switchBaseImage.getScaledInstance(switchW, switchH, Image.SCALE_SMOOTH);
                switchLabel.setIcon(new ImageIcon(scaledSwitch));
            }
            switchLabel.setVisible(false);
        }
    }

    private void setHeadAsync(String uuid) {
        executor.submit(() -> {
            try {
                java.net.URL url = new java.net.URL("https://mc-heads.net/avatar/" + uuid + "/64");
                ImageIcon icon = new ImageIcon(url);
                Image scaled = icon.getImage().getScaledInstance(48, 48, Image.SCALE_SMOOTH);
                Image switchScaled = icon.getImage().getScaledInstance(switchW, switchH, Image.SCALE_SMOOTH);
                SwingUtilities.invokeLater(() -> {
                    headLabel.setIcon(new ImageIcon(scaled));
                    switchLabel.setIcon(new ImageIcon(switchScaled));
                });
            } catch (Exception e) {
                log("Failed to load head: " + e.getMessage());
            }
        });
    }

    private record AccountEntry(String displayName, String uuid, String fileName) {
        @Override public String toString() { return displayName; }
    }

    private static class BackgroundPanel extends JPanel {
        private final Image image;
        private final int width;
        private final int height;

        BackgroundPanel(Image image, int width, int height) {
            this.image = image;
            this.width = width;
            this.height = height;
            setPreferredSize(new Dimension(width, height));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(image, 0, 0, width, height, this);
        }
    }

    private static class FadeOverlay extends JComponent {
        private float alpha;

        void setAlpha(float alpha) {
            this.alpha = Math.max(0f, Math.min(1f, alpha));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (alpha <= 0f) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.SrcOver.derive(alpha));
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
    }
}
