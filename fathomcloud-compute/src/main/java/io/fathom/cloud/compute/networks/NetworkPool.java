package io.fathom.cloud.compute.networks;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.server.model.Project;

import java.net.InetAddress;

public interface NetworkPool {
    InetAddress checkIpAvailable(long seed) throws CloudException;

    NetworkPoolAllocation reserveIp(Project project, InetAddress ip) throws CloudException;

    void releaseIpReservation(VirtualIp vip) throws CloudException;
}
