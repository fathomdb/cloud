package io.fathom.cloud.compute.networks;

import io.fathom.cloud.protobuf.CloudModel.VirtualIpData;
import io.fathom.cloud.protobuf.CloudModel.VirtualIpPoolData;

import java.net.InetAddress;
import java.util.Collections;
import java.util.List;

import org.bouncycastle.util.Arrays;

import com.google.common.collect.Lists;
import com.google.common.net.InetAddresses;

/**
 * A pool of EC2 floating IP addresses
 * 
 */
public class Ec2IpNetworkPool extends MappableIpNetworkPool {

    Ec2IpNetworkPool(NetworkPools networkPools, VirtualIpPoolData data) {
        super(networkPools, data);
    }

    @Override
    protected IpRange getIpRange() {
        // We override the methods that call getIpRange
        throw new UnsupportedOperationException();
    }

    @Override
    protected InetAddress convertSeedToIp(byte[] seed) {
        // Must be consistent, but this won't be meaningfully mapped to the
        // value
        int hashCode = Math.abs(Arrays.hashCode(seed));

        List<InetAddress> ips = getPoolIpAddresses();
        if (ips.isEmpty()) {
            throw new IllegalStateException("No EC2 IP addresses defined in pool");
        }

        return ips.get(hashCode % ips.size());
    }

    @Override
    protected List<InetAddress> getSystemReservedAddresses() {
        return Collections.emptyList();
    }

    private List<InetAddress> getPoolIpAddresses() {
        List<InetAddress> addresses = Lists.newArrayList();

        for (VirtualIpData ipData : poolData.getIpList()) {
            String ip = ipData.getIp();
            addresses.add(InetAddresses.forString(ip));
        }

        return addresses;
    }
}
