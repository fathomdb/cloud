package io.fathom.cloud.mq;

import io.fathom.cloud.ssh.SshConfig;

import java.io.IOException;

public interface MessageQueueService {
    // TODO: Passing in SSH config makes a bit of a mess of this of interface
    MessageQueueWriter getWriter(SshConfig sshConfig, String queue) throws IOException;

}
