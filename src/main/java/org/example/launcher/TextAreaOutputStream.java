package org.example.launcher;

import javax.swing.*;
import java.io.IOException;
import java.io.OutputStream;

class TextAreaOutputStream extends OutputStream {
    private final JTextArea area;

    TextAreaOutputStream(JTextArea area) {
        this.area = area;
    }

    @Override
    public void write(int b) throws IOException {
        append(new String(new byte[]{(byte) b}));
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        append(new String(b, off, len));
    }

    private void append(String s) {
        SwingUtilities.invokeLater(() -> {
            area.append(s);
            area.setCaretPosition(area.getDocument().getLength());
        });
    }
}
