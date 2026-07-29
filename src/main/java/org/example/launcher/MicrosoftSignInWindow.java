package org.example.launcher;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebView;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.util.concurrent.atomic.AtomicReference;

final class MicrosoftSignInWindow {
    private static final AtomicReference<MicrosoftSignInWindow> ACTIVE = new AtomicReference<>();
    private static final int WINDOW_WIDTH = 1080;
    private static final int WINDOW_HEIGHT = 760;

    private final JFrame frame;
    private final JFXPanel webPanel;
    private final JLabel statusLabel;
    private final JTextField codeField;
    private final JTextField uriField;
    private WebView webView;
    private volatile String pendingDirectUri;

    private MicrosoftSignInWindow() {
        frame = new JFrame("AxialClient Microsoft Sign-In");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setResizable(true);
        frame.setAlwaysOnTop(false);
        frame.setLayout(new BorderLayout());

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int x = Math.max(0, (screen.width - WINDOW_WIDTH) / 2);
        int y = Math.max(0, (screen.height - WINDOW_HEIGHT) / 2);
        frame.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        frame.setLocation(x, y);

        JPanel top = new JPanel(new java.awt.GridLayout(3, 1, 0, 6));
        top.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        top.setBackground(new Color(18, 18, 24));

        statusLabel = new JLabel("Starting Microsoft sign-in...");
        statusLabel.setForeground(Color.WHITE);
        top.add(statusLabel);

        codeField = new JTextField();
        codeField.setEditable(false);
        top.add(codeField);

        uriField = new JTextField();
        uriField.setEditable(false);
        top.add(uriField);

        frame.add(top, BorderLayout.NORTH);

        webPanel = new JFXPanel();
        webPanel.setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT - 140));
        frame.add(webPanel, BorderLayout.CENTER);

        JPanel actions = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 8));
        JButton copyCode = new JButton("Copy code");
        copyCode.addActionListener(e -> {
            String code = codeField.getText();
            if (!code.isBlank()) {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new java.awt.datatransfer.StringSelection(code), null);
            }
        });
        JButton close = new JButton("Close");
        close.addActionListener(e -> close());
        actions.add(copyCode);
        actions.add(close);
        frame.add(actions, BorderLayout.SOUTH);

        Platform.runLater(() -> {
            webView = new WebView();
            webView.setContextMenuEnabled(false);
            webPanel.setScene(new Scene(webView));
            loadPendingUri();
        });
    }

    static MicrosoftSignInWindow show(String verificationUri, String directVerificationUri, String userCode) {
        MicrosoftSignInWindow window = new MicrosoftSignInWindow();
        MicrosoftSignInWindow previous = ACTIVE.getAndSet(window);
        if (previous != null) {
            previous.close();
        }
        window.update(verificationUri, directVerificationUri, userCode, "Open this sign-in page and use the code shown.");
        window.show();
        return window;
    }

    void update(String verificationUri, String directVerificationUri, String userCode, String status) {
        pendingDirectUri = directVerificationUri;
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(status);
            codeField.setText("Code: " + userCode);
            uriField.setText("URL: " + verificationUri);
        });
        Platform.runLater(this::loadPendingUri);
    }

    void close() {
        if (ACTIVE.compareAndSet(this, null)) {
            SwingUtilities.invokeLater(frame::dispose);
        } else {
            SwingUtilities.invokeLater(frame::dispose);
        }
    }

    private void show() {
        SwingUtilities.invokeLater(() -> frame.setVisible(true));
    }

    private void loadPendingUri() {
        if (webView != null && pendingDirectUri != null && !pendingDirectUri.isBlank()) {
            webView.getEngine().load(pendingDirectUri);
        }
    }
}
