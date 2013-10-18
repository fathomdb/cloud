package io.fathom.cloud.mq.filesystem;

import io.fathom.cloud.mq.MessageQueueReader;
import io.fathom.cloud.sftp.RemoteFile;
import io.fathom.cloud.sftp.Sftp;
import io.fathom.cloud.ssh.SftpChannel;
import io.fathom.cloud.ssh.SshConfig;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteStreams;

public class SftpMessageQueueReader implements MessageQueueReader {
    private static final Logger log = LoggerFactory.getLogger(SftpMessageQueueReader.class);

    private final RemoteFile queueDir;

    private final SshConfig sshConfig;

    private Sftp buildSftp() throws IOException {
        SftpChannel sftp = sshConfig.getSftpChannel();
        return new Sftp(sftp, null);
    }

    public SftpMessageQueueReader(SshConfig sshConfig, RemoteFile queueDir) throws IOException {
        this.sshConfig = sshConfig;
        this.queueDir = queueDir;
    }

    @Override
    public byte[] poll() throws IOException {
        log.warn("Running request-poll against SFTP, likely not correct");

        try (SftpChannel sftp = buildSftp()) {
            List<String> names = sftp.ls(queueDir.getSshPath());
            if (names.isEmpty()) {
                return null;
            }

            Collections.sort(names);

            for (String name : names) {
                char firstChar = name.charAt(0);
                if (firstChar == '_' || firstChar == '.') {
                    continue;
                }
                byte[] data;
                RemoteFile file = new RemoteFile(queueDir, name);
                try (InputStream is = sftp.open(file.getSshPath())) {
                    data = ByteStreams.toByteArray(is);
                }

                sftp.delete(file.getSshPath());

                return data;
            }
        }

        return null;
    }

}
