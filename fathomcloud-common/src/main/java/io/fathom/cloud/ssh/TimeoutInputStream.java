package io.fathom.cloud.ssh;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimeoutInputStream extends InputStream {
    private static final Logger log = LoggerFactory.getLogger(TimeoutInputStream.class);

    final InputStream inner;
    int timeout = 0;

    public TimeoutInputStream(InputStream inner) {
        super();
        this.inner = inner;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        // Not clear whether we need this or not... Apache HttpClient is pulling
        // some weird stuff...
        log.warn("setTimeout not implemented (not needed???)");
        this.timeout = timeout;
    }

    @Override
    public int read() throws IOException {
        checkZeroTimeout();
        return inner.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        checkZeroTimeout();
        return inner.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        checkZeroTimeout();
        return inner.read(b, off, len);
    }

    private void checkZeroTimeout() throws IOException {
        int available = inner.available();

        if (available != 0) {
            return;
        }

        if (timeout == 0) {
            // No timeout
            return;
        }

        if (timeout < 5) {
            // Treat as zero timeout
            throw new SocketTimeoutException();
        } else {
            // TODO: Polling?
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public long skip(long n) throws IOException {
        return inner.skip(n);
    }

    @Override
    public int available() throws IOException {
        return inner.available();
    }

    @Override
    public void close() throws IOException {
        inner.close();
    }

    @Override
    public synchronized void mark(int readlimit) {
        inner.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        inner.reset();
    }

    @Override
    public boolean markSupported() {
        return inner.markSupported();
    }

}
