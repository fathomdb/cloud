package io.fathom.cloud.ssh;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.scheme.SchemeSocketFactory;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

public class SshTunnelSchemeSocketFactory implements SchemeSocketFactory {

    final SshContext sshContext;

    public SshTunnelSchemeSocketFactory(SshContext sshContext) {
        super();
        this.sshContext = sshContext;
    }

    @Override
    public Socket createSocket(final HttpParams params) throws IOException {
        return new SshTunnelSocket(sshContext);
    }

    @Override
    public final boolean isSecure(Socket sock) throws IllegalArgumentException {
        // Apache HTTPD can't figure out that it's OK to use a secure route when
        // an insecure one is requested...

        // return true;
        return false;
    }

    @Override
    public Socket connectSocket(Socket sock, InetSocketAddress remoteAddress, InetSocketAddress localAddress,
            HttpParams params) throws IOException, UnknownHostException, ConnectTimeoutException {
        if (remoteAddress == null) {
            throw new IllegalArgumentException("Remote address may not be null");
        }
        if (params == null) {
            throw new IllegalArgumentException("HTTP parameters may not be null");
        }
        if (sock == null) {
            sock = createSocket(null);
        }
        if (localAddress != null) {
            sock.setReuseAddress(HttpConnectionParams.getSoReuseaddr(params));
            sock.bind(localAddress);
        }
        int connTimeout = HttpConnectionParams.getConnectionTimeout(params);
        int soTimeout = HttpConnectionParams.getSoTimeout(params);

        try {
            sock.setSoTimeout(soTimeout);
            sock.connect(remoteAddress, connTimeout);
        } catch (SocketTimeoutException ex) {
            throw new ConnectTimeoutException("Connect to " + remoteAddress + " timed out");
        }
        return sock;
    }

}