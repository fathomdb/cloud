package io.fathom.cloud.io;

import java.io.IOException;
import java.io.OutputStream;

import com.google.common.hash.Hasher;

public class HashingOutputStream extends DelegatingOutputStream {
    private final Hasher hasher;

    public HashingOutputStream(OutputStream inner, Hasher hasher) {
        super(inner);
        this.hasher = hasher;
    }

    @Override
    public void write(int b) throws IOException {
        super.write(b);
        hasher.putByte((byte) b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        super.write(b);
        hasher.putBytes(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        super.write(b, off, len);
        hasher.putBytes(b, off, len);
    }

}
