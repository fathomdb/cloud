package io.fathom.cloud.compute.networks;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.server.model.Project;

import java.net.InetAddress;

public interface NetworkPool {
    InetAddress checkIpAvailable(byte[] seed) throws CloudException;

    NetworkPoolAllocation markIpAllocated(Project project, InetAddress ip) throws CloudException;

    void markIpNotAllocated(VirtualIp vip) throws CloudException;
}
