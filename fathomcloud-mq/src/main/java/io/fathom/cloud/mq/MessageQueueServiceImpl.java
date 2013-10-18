package io.fathom.cloud.mq;

import io.fathom.cloud.mq.MessageQueueService;
import io.fathom.cloud.mq.MessageQueueWriter;
import io.fathom.cloud.mq.filesystem.SftpMessageQueueWriter;
import io.fathom.cloud.sftp.RemoteFile;
import io.fathom.cloud.ssh.SshConfig;

import java.io.File;
import java.io.IOException;

import javax.inject.Singleton;

@Singleton
public class MessageQueueServiceImpl implements MessageQueueService {

    @Override
    public MessageQueueWriter getWriter(SshConfig sshConfig, String queue) throws IOException {
        if (queue.startsWith("sftp://")) {
            File path = new File(queue.substring(7));
            return new SftpMessageQueueWriter(sshConfig, new RemoteFile(path));
        } else {
            throw new IllegalArgumentException();
        }
    }

}
