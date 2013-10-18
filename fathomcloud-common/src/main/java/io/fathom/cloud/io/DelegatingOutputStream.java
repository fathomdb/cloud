package io.fathom.cloud.io;

import java.io.IOException;
import java.io.OutputStream;

/**
 * We could derive from FilterOutputStream, but:
 * 
 * we still have to override write(byte[], int, int)
 * 
 * methods cross-call, which really messes with derived clases
 * 
 */
public class DelegatingOutputStream extends OutputStream {

    private final OutputStream out;

    public DelegatingOutputStream(OutputStream out) {
        this.out = out;
    }

    @Override
    public void write(byte[] b) throws IOException {
        out.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
    }

    @Override
    public void write(int b) throws IOException {
        out.write(b);
    }

    @Override
    public void flush() throws IOException {
        out.flush();
    }

    @Override
    public void close() throws IOException {
        out.close();
    }

}
