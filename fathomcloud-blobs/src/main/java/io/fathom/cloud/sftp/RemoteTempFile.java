package io.fathom.cloud.sftp;

import io.fathom.cloud.ssh.SftpChannel;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Random;

import com.fathomdb.utils.Hex;

public class RemoteTempFile implements Closeable {
    final SftpChannel sftp;
    RemoteFile file;

    RemoteTempFile(SftpChannel sftp, RemoteFile file) {
        this.sftp = sftp;
        this.file = file;
    }

    public File getSshPath() {
        return file.getSshPath();
    }

    @Override
    public void close() throws IOException {
        if (file != null) {
            sftp.delete(file.getSshPath());
            file = null;
        }
    }

    public void renameTo(RemoteFile dest) throws IOException {
        sftp.mv(file.getSshPath(), dest.getSshPath());
        file = null;
    }

    static final Random random = new Random();

    public static RemoteTempFile create(SftpChannel sftp, RemoteFile tmpdir) {
        while (true) {
            byte[] bytes = new byte[16];
            synchronized (random) {
                random.nextBytes(bytes);
            }
            String hex = Hex.toHex(bytes);

            RemoteFile f = new RemoteFile(tmpdir, hex);
            // We don't bother checking if the remote file exists...
            // if (f.exists()) {
            // log.warn("Found duplicate file in temp directory: " + f);
            // continue;
            // }
            // TODO: Technically a race :-(
            return new RemoteTempFile(sftp, f);
        }
    }
}
