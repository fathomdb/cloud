package io.fathom.auto.endpoint;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;

import com.google.common.net.InetAddresses;

public class InetSocketAddresses {

    public static InetSocketAddress parse(String s) {
        try {
            URI uri = new URI("http://" + s);
            String host = uri.getHost();
            int port = uri.getPort();

            if (host == null || port == -1) {
                throw new IllegalArgumentException("Unable to parse address: " + s);
            }

            InetAddress address = InetAddresses.forString(host);
            return new InetSocketAddress(address, port);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("Unable to parse address: " + s);
        }
    }

    public static String toString(InetSocketAddress addr) {
        return InetAddresses.toUriString(addr.getAddress()) + ":" + addr.getPort();
    }

}
