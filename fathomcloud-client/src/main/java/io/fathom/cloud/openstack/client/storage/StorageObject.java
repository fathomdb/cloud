package io.fathom.cloud.openstack.client.storage;

import io.fathom.cloud.openstack.client.SimpleRestClient;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

public class StorageObject implements Closeable {

    InputStream is;

    public StorageObject(InputStream is) {
        super();
        this.is = is;
    }

    @Override
    public void close() throws IOException {
        if (is != null) {
            is.close();
        }
    }

    public InputStream getDataInputStream() {
        if (is == null) {
            throw new IllegalStateException();
        }
        InputStream ret = is;
        is = null;
        return ret;
    }

    public String getAsString() throws IOException {
        return new String(getAsByteArray(), SimpleRestClient.UTF_8);
    }

    public byte[] getAsByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InputStream is = getDataInputStream();
        try {
            byte[] buffer = new byte[4096];

            while (true) {
                int n = is.read(buffer);
                if (n == -1) {
                    break;
                }
                baos.write(buffer, 0, n);
            }
            return baos.toByteArray();
        } finally {
            is.close();
        }
    }

}
