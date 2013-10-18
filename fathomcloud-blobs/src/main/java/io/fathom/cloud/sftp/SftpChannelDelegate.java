package io.fathom.cloud.sftp;

import io.fathom.cloud.ssh.SftpChannel;
import io.fathom.cloud.ssh.SftpStat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class SftpChannelDelegate implements SftpChannel {
    final SftpChannel inner;

    public SftpChannelDelegate(SftpChannel inner) {
        super();
        this.inner = inner;
    }

    @Override
    public InputStream open(File path) throws IOException {
        return inner.open(path);
    }

    @Override
    public OutputStream writeFile(File path, WriteMode mode) throws IOException {
        return inner.writeFile(path, mode);
    }

    @Override
    public void delete(File path) throws IOException {
        inner.delete(path);
    }

    @Override
    public SftpStat stat(File path) throws IOException {
        return inner.stat(path);
    }

    @Override
    public boolean mkdirs(File path) throws IOException {
        return inner.mkdirs(path);
    }

    @Override
    public void mv(File from, File to) throws IOException {
        inner.mv(from, to);
    }

    @Override
    public void chmod(File file, int mode) throws IOException {
        inner.chmod(file, mode);
    }

    @Override
    public boolean exists(File file) throws IOException {
        return inner.exists(file);
    }

    @Override
    public byte[] readAllBytes(File file) throws IOException {
        return inner.readAllBytes(file);
    }

    @Override
    public List<String> ls(File file) throws IOException {
        return inner.ls(file);
    }

    @Override
    public void close() throws IOException {
        inner.close();
    }

    @Override
    public void chown(File file, int uid) throws IOException {
        inner.chown(file, uid);
    }

    @Override
    public void chgrp(File file, int gid) throws IOException {
        inner.chgrp(file, gid);
    }

}
