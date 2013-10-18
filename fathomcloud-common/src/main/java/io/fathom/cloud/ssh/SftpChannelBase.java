package io.fathom.cloud.ssh;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import com.google.common.io.ByteStreams;

public abstract class SftpChannelBase implements SftpChannel {
    @Override
    public boolean mkdirs(File file) throws IOException {
        try {
            return mkdir(file);
        } catch (FileNotFoundException e) {
            File parent = file.getParentFile();
            if (parent == null) {
                throw new IOException("Error during sftp mkdirs (reached root)");
            }
            mkdirs(parent);
            return mkdir(file);
        }
    }

    @Override
    public byte[] readAllBytes(File file) throws IOException {
        try (InputStream is = open(file)) {
            return ByteStreams.toByteArray(is);
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    public abstract boolean mkdir(File file) throws IOException;

}
