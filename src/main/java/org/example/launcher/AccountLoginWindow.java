package org.example.launcher;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FlowLayout;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class AccountLoginWindow {
    private final JFrame frame;
    private final JLabel statusLabel = new JLabel("Opening Microsoft sign-in...");
    private final JButton closeButton = new JButton("Close");
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Path accountsDir = ClientPaths.accountsDir();
    private final Path activeAccountPointer = ClientPaths.activeAccountPointer();

    private AccountLoginWindow() {
        frame = new JFrame("AxialClient Sign In");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setResizable(false);
        frame.setSize(480, 180);
        frame.setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(new Color(14, 16, 22));
        root.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        frame.setContentPane(root);

        JLabel title = new JLabel("Microsoft Sign-In", SwingConstants.LEFT);
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        root.add(title, BorderLayout.NORTH);

        statusLabel.setForeground(new Color(190, 198, 220));
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        root.add(statusLabel, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        footer.setOpaque(false);
        closeButton.addActionListener(e -> frame.dispose());
        footer.add(closeButton);
        root.add(footer, BorderLayout.SOUTH);

        startLogin();
    }

    static void show() {
        SwingUtilities.invokeLater(() -> {
            AccountLoginWindow window = new AccountLoginWindow();
            window.frame.setVisible(true);
        });
    }

    private void startLogin() {
        executor.submit(() -> {
            try {
                Files.createDirectories(accountsDir);
                Path temp = accountsDir.resolve("temp.json");
                Files.deleteIfExists(temp);

                AuthManager auth = new AuthManager(temp);
                AuthResult result = auth.authenticate();

                Path target = accountsDir.resolve(result.playerName + ".json");
                Files.deleteIfExists(target);
                Files.move(temp, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                writeActiveAccountPointer(target.getFileName().toString());

                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Signed in as " + result.playerName + ".");
                    frame.dispose();
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> statusLabel.setText("Login failed: " + ex.getMessage()));
            }
        });
    }

    private void writeActiveAccountPointer(String fileName) {
        try {
            Files.createDirectories(activeAccountPointer.getParent());
            Files.writeString(activeAccountPointer, fileName);
        } catch (IOException ignored) {
        }
    }
}
