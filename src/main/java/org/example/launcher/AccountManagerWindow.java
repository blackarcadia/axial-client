package org.example.launcher;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javafx.embed.swing.JFXPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

final class AccountManagerWindow {
    private static final AtomicBoolean JAVAFX_INITIALIZED = new AtomicBoolean();
    private final JFrame frame;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final DefaultListModel<AccountEntry> model = new DefaultListModel<>();
    private final JList<AccountEntry> accountList = new JList<>(model);
    private final JButton addAccountButton = new JButton("Add Account");
    private final JButton logoutButton = new JButton("Log Out");
    private final JButton closeButton = new JButton("Close");
    private final Path accountsDir = ClientPaths.accountsDir();
    private final Path activeAccountPointer = ClientPaths.activeAccountPointer();
    private final JLabelPanel headerPanel = new JLabelPanel();

    private AccountManagerWindow() {
        frame = new JFrame("AxialClient Accounts");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setResizable(false);
        frame.setSize(1040, 720);
        frame.setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(new Color(14, 16, 22));
        frame.setContentPane(root);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.setBorder(BorderFactory.createEmptyBorder(18, 20, 12, 20));

        Font titleFont = new Font("SansSerif", Font.BOLD, 28);
        Font subtitleFont = new Font("SansSerif", Font.PLAIN, 13);
        headerPanel.title.setText("Accounts");
        headerPanel.title.setFont(titleFont);
        headerPanel.title.setForeground(Color.WHITE);
        headerPanel.subtitle.setText("Manage the accounts that can be used in Axial.");
        headerPanel.subtitle.setFont(subtitleFont);
        headerPanel.subtitle.setForeground(new Color(190, 198, 220));
        top.add(headerPanel, BorderLayout.WEST);
        root.add(top, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(18, 0));
        center.setOpaque(false);
        center.setBorder(BorderFactory.createEmptyBorder(0, 20, 20, 20));
        root.add(center, BorderLayout.CENTER);

        accountList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        accountList.setCellRenderer(new AccountRenderer());
        accountList.setFixedCellHeight(58);
        accountList.setOpaque(false);
        accountList.setBackground(new Color(0, 0, 0, 0));
        accountList.setForeground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(accountList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(new Color(0, 0, 0, 0));
        scrollPane.setOpaque(false);
        scrollPane.setPreferredSize(new Dimension(560, 560));
        center.add(scrollPane, BorderLayout.CENTER);

        JPanel side = new JPanel(new GridBagLayout());
        side.setOpaque(false);
        side.setPreferredSize(new Dimension(360, 560));
        center.add(side, BorderLayout.EAST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 12));
        actions.setOpaque(false);

        stylePrimaryButton(addAccountButton);
        styleSecondaryButton(logoutButton);
        styleSecondaryButton(closeButton);

        addAccountButton.addActionListener(e -> loginNewAccount());
        logoutButton.addActionListener(e -> logoutSelected());
        closeButton.addActionListener(e -> frame.dispose());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        side.add(makeSideCard(), gbc);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footer.setOpaque(false);
        footer.add(addAccountButton);
        footer.add(logoutButton);
        footer.add(closeButton);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.setBorder(BorderFactory.createEmptyBorder(0, 20, 20, 20));
        bottom.add(footer, BorderLayout.EAST);
        root.add(bottom, BorderLayout.SOUTH);

        refreshAccounts();
    }

    static void show() {
        ensureJavaFx();
        SwingUtilities.invokeLater(() -> {
            AccountManagerWindow window = new AccountManagerWindow();
            window.frame.setVisible(true);
        });
    }

    private JPanel makeSideCard() {
        JPanel card = new JPanel(new BorderLayout(0, 14));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(82, 92, 120), 1),
                BorderFactory.createEmptyBorder(18, 18, 18, 18)
        ));
        card.setBackground(new Color(22, 25, 33));
        card.setPreferredSize(new Dimension(360, 200));

        JLabelPanel info = new JLabelPanel();
        info.title.setText("Selected Account");
        info.title.setForeground(Color.WHITE);
        info.title.setFont(new Font("SansSerif", Font.BOLD, 18));
        info.subtitle.setText("Choose one account to use on next launch.");
        info.subtitle.setForeground(new Color(190, 198, 220));
        card.add(info, BorderLayout.NORTH);

        JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        card.add(spacer, BorderLayout.CENTER);

        return card;
    }

    private void refreshAccounts() {
        model.clear();
        List<AccountEntry> entries = new ArrayList<>();
        if (Files.isDirectory(accountsDir)) {
            try (var stream = Files.list(accountsDir)) {
                stream.filter(p -> p.toString().endsWith(".json"))
                        .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                        .forEach(p -> {
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
            model.addElement(new AccountEntry("No accounts yet", "", ""));
            accountList.setEnabled(false);
            logoutButton.setEnabled(false);
        } else {
            accountList.setEnabled(true);
            logoutButton.setEnabled(true);
            for (AccountEntry entry : entries) {
                model.addElement(entry);
            }
            String active = readActiveAccountPointer();
            if (active != null) {
                for (int i = 0; i < model.size(); i++) {
                    if (active.equals(model.get(i).fileName)) {
                        accountList.setSelectedIndex(i);
                        break;
                    }
                }
            }
            if (accountList.getSelectedIndex() < 0) {
                accountList.setSelectedIndex(0);
            }
        }
    }

    private void loginNewAccount() {
        ensureJavaFx();
        addAccountButton.setEnabled(false);
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
                SwingUtilities.invokeLater(this::refreshAccounts);
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(frame, ex.getMessage(), "Login failed", JOptionPane.ERROR_MESSAGE));
            } finally {
                SwingUtilities.invokeLater(() -> addAccountButton.setEnabled(true));
            }
        });
    }

    private void logoutSelected() {
        AccountEntry selected = accountList.getSelectedValue();
        if (selected == null || selected.fileName == null || selected.fileName.isBlank()) {
            return;
        }

        try {
            Files.deleteIfExists(accountsDir.resolve(selected.fileName));
            if (selected.fileName.equals(readActiveAccountPointer())) {
                Files.deleteIfExists(activeAccountPointer);
            }
            refreshAccounts();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(frame, ex.getMessage(), "Logout failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String readActiveAccountPointer() {
        try {
            if (Files.exists(activeAccountPointer)) {
                String value = Files.readString(activeAccountPointer).trim();
                if (!value.isBlank()) {
                    return value;
                }
            }
        } catch (IOException ignored) {
        }
        return null;
    }

    private void writeActiveAccountPointer(String fileName) {
        try {
            Files.createDirectories(activeAccountPointer.getParent());
            Files.writeString(activeAccountPointer, fileName);
        } catch (IOException ignored) {
        }
    }

    private static void stylePrimaryButton(JButton button) {
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBackground(new Color(52, 96, 255));
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
    }

    private static void styleSecondaryButton(JButton button) {
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBackground(new Color(34, 39, 52));
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
    }

    private static void ensureJavaFx() {
        if (JAVAFX_INITIALIZED.compareAndSet(false, true)) {
            new JFXPanel();
        }
    }

    private record AccountEntry(String displayName, String uuid, String fileName) {
        @Override
        public String toString() {
            return displayName;
        }
    }

    private static final class AccountRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabelPanel panel = new JLabelPanel();
            panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createEmptyBorder(6, 6, 6, 6),
                    BorderFactory.createLineBorder(isSelected ? new Color(92, 132, 255) : new Color(46, 52, 68), 1)
            ));
            panel.setBackground(isSelected ? new Color(38, 45, 62) : new Color(22, 25, 33));
            panel.title.setForeground(Color.WHITE);
            panel.subtitle.setForeground(new Color(186, 194, 212));

            if (value instanceof AccountEntry entry) {
                panel.title.setText(entry.displayName);
                panel.subtitle.setText(entry.fileName.isBlank() ? "No saved profile" : "Stored account");
            } else {
                panel.title.setText(String.valueOf(value));
                panel.subtitle.setText("");
            }

            return panel;
        }
    }

    private static final class JLabelPanel extends JPanel {
        private final javax.swing.JLabel title = new javax.swing.JLabel();
        private final javax.swing.JLabel subtitle = new javax.swing.JLabel();

        private JLabelPanel() {
            super(new GridBagLayout());
            setOpaque(true);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            add(title, gbc);
            gbc.gridy = 1;
            add(subtitle, gbc);
        }
    }
}
