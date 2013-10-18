package io.fathom.cloud.ssh;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.SocketFactory;

public class SshTunnelSocketFactory extends SocketFactory {
    final SshContext sshContext;

    public SshTunnelSocketFactory(SshContext sshContext) {
        super();
        this.sshContext = sshContext;
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
        return createSocket(new InetSocketAddress(host, port));
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException,
            UnknownHostException {
        return createSocket(new InetSocketAddress(host, port), new InetSocketAddress(localHost, localPort));
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return createSocket(new InetSocketAddress(host, port));
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort)
            throws IOException {
        return createSocket(new InetSocketAddress(address, port), new InetSocketAddress(localAddress, localPort));
    }

    private Socket createSocket(InetSocketAddress remote, InetSocketAddress local) throws IOException {
        Socket socket = new SshTunnelSocket(sshContext, local);
        socket.connect(remote);
        return socket;
    }

    private Socket createSocket(InetSocketAddress remote) throws IOException {
        Socket socket = new SshTunnelSocket(sshContext);
        socket.connect(remote);
        return socket;
    }
}
