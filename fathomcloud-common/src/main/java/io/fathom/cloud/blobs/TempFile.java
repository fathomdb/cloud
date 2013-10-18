package io.fathom.cloud.blobs;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteSink;
import com.google.common.io.ByteSource;
import com.google.common.io.FileWriteMode;
import com.google.common.io.Files;

public class TempFile implements Closeable {
    private static final Logger log = LoggerFactory.getLogger(TempFile.class);

    private File file;

    public File getFile() {
        if (file == null) {
            throw new IllegalStateException();
        }
        return file;
    }

    public TempFile(File file) {
        this.file = file;
    }

    public static TempFile in(File dir) throws IOException {
        return new TempFile(File.createTempFile("tmp", "dat", dir));
    }

    public static TempFile create() throws IOException {
        return in(null);
    }

    @Override
    public void close() throws IOException {
        if (file != null) {
            if (!file.delete()) {
                log.warn("Unable to delete temp file: {}", file);
            }
            file = null;
        }
    }

    public boolean renameTo(File dest) {
        boolean ret = getFile().renameTo(dest);
        if (ret) {
            file = null;
        }
        return ret;
    }

    public void copyFrom(ByteSource data) throws IOException {
        data.copyTo(asByteSink(false));
    }

    public ByteSink asByteSink(boolean append) {
        if (append) {
            return Files.asByteSink(getFile(), FileWriteMode.APPEND);
        } else {
            return Files.asByteSink(getFile());
        }
    }

    public ByteSource asByteSource() {
        return Files.asByteSource(getFile());
    }

}
