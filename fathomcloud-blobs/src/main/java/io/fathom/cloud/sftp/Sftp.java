package io.fathom.cloud.sftp;

import io.fathom.cloud.ssh.SftpChannel;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.google.common.io.ByteSource;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;

public class Sftp extends SftpChannelDelegate {

    private final RemoteFile tmpdir;

    public Sftp(SftpChannel inner, RemoteFile tmpdir) {
        super(inner);
        this.tmpdir = tmpdir;
    }

    public void writeAtomic(RemoteFile remoteFile, File src) throws IOException {
        writeAtomic(remoteFile, Files.asByteSource(src));
    }

    public void writeAtomic(RemoteFile remoteFile, byte[] src) throws IOException {
        writeAtomic(remoteFile, ByteStreams.asByteSource(src));
    }

    public void writeAtomic(RemoteFile remoteFile, ByteSource src) throws IOException {
        writeAtomic(remoteFile, src, null);
    }

    public void writeAtomic(RemoteFile remoteFile, ByteSource src, Integer chmod) throws IOException {
        try (RemoteTempFile remoteTemp = buildRemoteTemp()) {
            try (OutputStream os = writeFile(remoteTemp.getSshPath(), WriteMode.Overwrite)) {
                src.copyTo(os);
            }

            if (chmod != null) {
                this.chmod(remoteTemp.getSshPath(), chmod);
            }

            remoteTemp.renameTo(remoteFile);
        }
    }

    public RemoteTempFile buildRemoteTemp() {
        return RemoteTempFile.create(inner, tmpdir);
    }

    public void copy(RemoteFile src, File dest) throws IOException {
        try (InputStream is = open(src.getSshPath())) {
            ByteStreams.copy(is, Files.newOutputStreamSupplier(dest));
        }
    }

    // public void writeAtomic(RemoteFile remoteFile, InputSupplier<? extends
    // InputStream> from) throws IOException {
    // try (RemoteTempFile remoteTemp = buildRemoteTemp()) {
    // try (OutputStream os = writeFile(remoteTemp.getSshPath(),
    // WriteMode.Overwrite)) {
    // ByteStreams.copy(from, os);
    // }
    //
    // remoteTemp.renameTo(remoteFile);
    // }
    // }

}
