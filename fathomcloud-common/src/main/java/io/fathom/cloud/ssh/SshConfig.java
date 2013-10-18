package io.fathom.cloud.ssh;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public interface SshConfig {
    SftpChannel getSftpChannel() throws IOException;

    SshDirectTcpipChannel getDirectTcpipConnection(InetSocketAddress local, InetSocketAddress remote)
            throws IOException;

    int execute(String command, OutputStream stdout, OutputStream stderr) throws IOException;

    String getUser();

    SshForwardChannel forwardLocalPort(InetAddress localAddress, InetSocketAddress remoteSocketAddress)
            throws IOException;
}
