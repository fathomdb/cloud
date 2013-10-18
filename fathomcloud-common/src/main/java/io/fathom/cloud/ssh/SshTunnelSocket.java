package io.fathom.cloud.ssh;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketImpl;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.InetAddresses;

/**
 * A Socket implementation that uses SSH 'direct-tcpip' forwarding.
 * 
 * By implementing Socket, we avoid opening a local port for forwarding.
 * 
 */
public class SshTunnelSocket extends Socket {
    private static final Logger log = LoggerFactory.getLogger(SshTunnelSocket.class);

    private final SshTunnelSocketImpl impl;

    private SshTunnelSocket(SshTunnelSocketImpl impl) throws IOException {
        super(impl);
        this.impl = impl;
    }

    public SshTunnelSocket(SshContext sshContext, InetSocketAddress localSocketAddress) throws IOException {
        this(new SshTunnelSocketImpl(sshContext, localSocketAddress));
    }

    public SshTunnelSocket(SshContext sshContext) throws IOException {
        this(sshContext, generateLocalSocketAddress());
    }

    private static InetSocketAddress generateLocalSocketAddress() throws UnknownHostException {
        int port = 30000;
        InetAddress localAddress = InetAddress.getLocalHost();
        return new InetSocketAddress(localAddress, port);
    }

    static class SshTunnelSocketImpl extends SocketImpl {
        private final SshContext sshContext;
        private final InetSocketAddress localSocketAddress;

        private SshDirectTcpipChannel channel;

        SshConfig sshConfig;
        private int timeoutOption = 60000;
        private TimeoutInputStream in;

        public SshTunnelSocketImpl(SshContext sshContext, InetSocketAddress localSocketAddress) {
            this.sshContext = sshContext;
            this.localSocketAddress = localSocketAddress;
        }

        @Override
        public void setOption(int opt, Object value) throws SocketException {
            log.warn("Ignoring setOption {} {}", opt, value);

            if (opt == SO_TIMEOUT) {
                this.timeoutOption = (int) value;
                if (in != null) {
                    in.setTimeout(timeoutOption);
                }
            }
        }

        @Override
        public Object getOption(int opt) throws SocketException {
            if (opt == SO_TIMEOUT) {
                return new Integer(timeoutOption);
            }

            // int ret = 0;
            // /*
            // * The native socketGetOption() knows about 3 options.
            // * The 32 bit value it returns will be interpreted according
            // * to what we're asking. A return of -1 means it understands
            // * the option but its turned off. It will raise a SocketException
            // * if "opt" isn't one it understands.
            // */
            //
            // switch (opt) {
            // case TCP_NODELAY:
            // ret = socketGetOption(opt, null);
            // return Boolean.valueOf(ret != -1);
            // case SO_OOBINLINE:
            // ret = socketGetOption(opt, null);
            // return Boolean.valueOf(ret != -1);
            // case SO_LINGER:
            // ret = socketGetOption(opt, null);
            // return (ret == -1) ? Boolean.FALSE: (Object)(new Integer(ret));
            // case SO_REUSEADDR:
            // ret = socketGetOption(opt, null);
            // return Boolean.valueOf(ret != -1);
            // case SO_BINDADDR:
            // InetAddressContainer in = new InetAddressContainer();
            // ret = socketGetOption(opt, in);
            // return in.addr;
            // case SO_SNDBUF:
            // case SO_RCVBUF:
            // ret = socketGetOption(opt, null);
            // return new Integer(ret);
            // case IP_TOS:
            // ret = socketGetOption(opt, null);
            // if (ret == -1) { // ipv6 tos
            // return new Integer(trafficClass);
            // } else {
            // return new Integer(ret);
            // }
            // case SO_KEEPALIVE:
            // ret = socketGetOption(opt, null);
            // return Boolean.valueOf(ret != -1);
            // // should never get here
            // default:
            // return null;
            // }
            throw new UnsupportedOperationException();
        }

        @Override
        protected void create(boolean stream) throws IOException {
        }

        @Override
        protected void connect(String host, int port) throws IOException {
            InetAddress remoteAddr = InetAddress.getByName(host);
            connect(remoteAddr, port);
        }

        @Override
        protected void connect(InetAddress address, int port) throws IOException {
            InetSocketAddress remote = new InetSocketAddress(address, port);
            connect(remote, 0);
        }

        @Override
        protected void connect(SocketAddress address, int timeout) throws IOException {
            InetSocketAddress httpAddress = (InetSocketAddress) address;

            InetSocketAddress sshServer = sshContext.getRemoteSshAddress(httpAddress);

            SshDirectTcpipChannel channel;
            try {
                if (this.sshConfig != null) {
                    throw new IllegalStateException();
                }
                this.sshConfig = sshContext.buildConfig(sshServer);

                InetSocketAddress tunnelRemote = new InetSocketAddress(InetAddresses.forString("127.0.0.1"),
                        httpAddress.getPort());

                channel = sshConfig.getDirectTcpipConnection(localSocketAddress, tunnelRemote);
            } catch (Exception e) {
                throw new IOException("Error connecting channel", e);
            }

            this.channel = channel;
        }

        @Override
        protected void bind(InetAddress host, int port) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void listen(int backlog) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void accept(SocketImpl s) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        protected InputStream getInputStream() throws IOException {
            synchronized (this) {
                if (in == null) {
                    in = new TimeoutInputStream(channel.getInputStream());
                    in.setTimeout(timeoutOption);
                }
                return in;
            }
        }

        @Override
        protected OutputStream getOutputStream() throws IOException {
            return channel.getOutputStream();
        }

        @Override
        protected int available() throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void close() throws IOException {
            if (channel != null) {
                channel.close();
            }
        }

        @Override
        protected void sendUrgentData(int data) throws IOException {
            throw new UnsupportedOperationException();
        }
    }

}
