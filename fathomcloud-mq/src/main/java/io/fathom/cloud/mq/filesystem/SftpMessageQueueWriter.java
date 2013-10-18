package io.fathom.cloud.mq.filesystem;

import io.fathom.cloud.mq.MessageQueueWriter;
import io.fathom.cloud.sftp.RemoteFile;
import io.fathom.cloud.sftp.Sftp;
import io.fathom.cloud.ssh.SftpChannel;
import io.fathom.cloud.ssh.SshConfig;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SftpMessageQueueWriter implements MessageQueueWriter {
    private static final Logger log = LoggerFactory.getLogger(SftpMessageQueueWriter.class);

    private final RemoteFile queueDir;

    private final SshConfig sshConfig;

    private final RemoteFile tmpDir;

    private Sftp buildSftp() throws IOException {
        SftpChannel sftp = sshConfig.getSftpChannel();
        return new Sftp(sftp, tmpDir);
    }

    public SftpMessageQueueWriter(SshConfig sshConfig, RemoteFile queueDir) throws IOException {
        this.sshConfig = sshConfig;
        this.queueDir = queueDir;

        this.tmpDir = new RemoteFile(queueDir, "_tmp");
        try (Sftp sftp = buildSftp()) {
            sftp.mkdirs(tmpDir.getSshPath());
        }
    }

    @Override
    public void enqueue(byte[] data) throws IOException {
        String name = System.currentTimeMillis() + "_" + UUID.randomUUID().toString();

        try (Sftp sftp = buildSftp()) {
            RemoteFile remoteFile = new RemoteFile(queueDir, name);

            sftp.writeAtomic(remoteFile, data);
        }
    }

}
