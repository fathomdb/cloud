package io.fathom.auto.endpoint;

import java.net.InetSocketAddress;

import com.google.common.base.Strings;

public class Endpoint {

    private final EndpointData data;

    public Endpoint(EndpointData data) {
        this.data = data;
    }

    public InetSocketAddress getAddress() {
        String address = data.address;
        if (Strings.isNullOrEmpty(address)) {
            return null;
        }

        return InetSocketAddresses.parse(address);
    }

}
