package io.fathom.cloud.compute.networks;

import io.fathom.cloud.CloudException;

import java.net.InetAddress;

public abstract class NetworkPoolAllocation {

    public abstract InetAddress getAddress();

    public abstract void releaseIp() throws CloudException;
}
