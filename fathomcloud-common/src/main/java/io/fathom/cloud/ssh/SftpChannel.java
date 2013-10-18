package io.fathom.cloud.ssh;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public interface SftpChannel extends SshChannel {
    /**
     * Opens an InputStream onto the file. If not found, throws
     * FileNotFoundException.
     */
    InputStream open(File path) throws IOException;

    OutputStream writeFile(File path, WriteMode mode) throws IOException;

    void delete(File path) throws IOException;

    SftpStat stat(File path) throws IOException;

    boolean mkdirs(File path) throws IOException;

    void mv(File from, File to) throws IOException;

    boolean exists(File file) throws IOException;

    /**
     * Reads the contents of a file (watch out for big files!) If the file is
     * not found, returns null
     */
    byte[] readAllBytes(File file) throws IOException;

    List<String> ls(File file) throws IOException;

    enum WriteMode {
        Append, Overwrite
    }

    void chmod(File file, int mode) throws IOException;

    void chown(File file, int uid) throws IOException;

    void chgrp(File file, int gid) throws IOException;
}
