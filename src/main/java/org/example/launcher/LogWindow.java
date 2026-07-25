package org.example.launcher;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Simple Swing log sink so double-clicked runs show activity.
 */
public class LogWindow {
    private final JFrame frame;
    private final JTextArea area;
    private final PrintStream stream;

    public LogWindow() {
        frame = new JFrame("AxialClient Version 1.0 Alphatest");
        setFrameIcon(frame);
        area = new JTextArea(18, 70);
        area.setEditable(false);
        DefaultCaret caret = (DefaultCaret) area.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.add(new JScrollPane(area), BorderLayout.CENTER);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        OutputStream os = new OutputStream() {
            @Override public void write(int b) { append(new String(new byte[]{(byte) b})); }
            @Override public void write(byte[] b, int off, int len) { append(new String(b, off, len)); }
            private void append(String s) {
                SwingUtilities.invokeLater(() -> area.append(s));
            }
        };
        stream = new PrintStream(os, true);
    }

    public PrintStream getPrintStream() {
        return stream;
    }

    private void setFrameIcon(JFrame frame) {
        try {
            Image icon = javax.imageio.ImageIO.read(LogWindow.class.getResource("/app-icon.png"));
            frame.setIconImage(icon);
        } catch (Exception ignored) {
        }
    }
}
