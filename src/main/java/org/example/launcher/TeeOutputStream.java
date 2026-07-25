package org.example.launcher;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

public class TeeOutputStream extends OutputStream {
    private final OutputStream a;
    private final OutputStream b;

    public TeeOutputStream(OutputStream a, OutputStream b) {
        this.a = Objects.requireNonNull(a);
        this.b = Objects.requireNonNull(b);
    }

    @Override
    public void write(int i) throws IOException {
        a.write(i);
        b.write(i);
    }

    @Override
    public void write(byte[] bytes, int off, int len) throws IOException {
        a.write(bytes, off, len);
        b.write(bytes, off, len);
    }

    @Override
    public void flush() throws IOException {
        a.flush();
        b.flush();
    }

    @Override
    public void close() throws IOException {
        a.close();
        b.close();
    }
}
