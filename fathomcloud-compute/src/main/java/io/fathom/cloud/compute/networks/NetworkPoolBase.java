package io.fathom.cloud.compute.networks;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.server.model.Project;

import java.net.InetAddress;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.net.InetAddresses;

public abstract class NetworkPoolBase implements NetworkPool {

    private static final Logger log = LoggerFactory.getLogger(NetworkPoolBase.class);

    public abstract String getNetworkKey();

    protected abstract NetworkPoolAllocation markIpAllocated0(Project project, InetAddress ip) throws CloudException;

    protected abstract InetAddress getGateway();

    protected abstract List<InetAddress> getAllocatedIps() throws CloudException;

    protected Set<String> getAddressesInUse() throws CloudException {
        Set<String> reserved = Sets.newHashSet();

        for (InetAddress ip : getSystemReservedAddresses()) {
            reserved.add(InetAddresses.toAddrString(ip));
        }

        for (InetAddress ip : getAllocatedIps()) {
            reserved.add(InetAddresses.toAddrString(ip));
        }

        return reserved;
    }

    protected List<InetAddress> getSystemReservedAddresses() {
        IpRange ipRange = getIpRange();

        // TODO: Anything else reserved? Broadcast address? Configurable?

        byte[] mask = ipRange.getNetmaskBytes();
        byte[] prefix = ipRange.getAddress().getAddress();

        List<InetAddress> reserved = Lists.newArrayList();

        {
            // Reserve the router/host address
            reserved.add(ipRange.getAddress());
        }

        {
            // Reserve the gateway
            InetAddress gateway = getGateway();
            if (gateway != null) {
                reserved.add(gateway);
            }
        }

        {
            // Reserve the .0 address
            byte[] ip = new byte[prefix.length];
            System.arraycopy(prefix, 0, ip, 0, ip.length);
            for (int i = 0; i < mask.length; i++) {
                ip[i] &= mask[i];
            }
            reserved.add(NetworkPools.toAddress(ip));
        }

        {
            // Reserve the broadcast address
            byte[] ip = new byte[prefix.length];
            System.arraycopy(prefix, 0, ip, 0, ip.length);
            for (int i = 0; i < mask.length; i++) {
                ip[i] &= mask[i];
            }
            for (int i = 0; i < mask.length; i++) {
                ip[i] |= ~mask[i];
            }
            InetAddress addr = NetworkPools.toAddress(ip);
            reserved.add(addr);
            log.debug("Reserved broadcast address in {}: {}", ipRange, addr);
        }

        return reserved;
    }

    protected abstract IpRange getIpRange();

    @Override
    public InetAddress checkIpAvailable(byte[] seed) throws CloudException {
        InetAddress address = convertSeedToIp(seed);

        Set<String> reserved = getAddressesInUse();

        if (reserved.contains(InetAddresses.toAddrString(address))) {
            return null;
        }

        return address;
    }

    protected InetAddress convertSeedToIp(byte[] seed) {
        IpRange ipRange = getIpRange();

        byte[] mask = ipRange.getNetmaskBytes();
        byte[] prefix = ipRange.getAddress().getAddress();
        byte[] ip = new byte[mask.length];
        System.arraycopy(seed, seed.length - ip.length, ip, 0, ip.length);

        // Apply netmask
        for (int i = 0; i < mask.length; i++) {
            ip[i] &= ~mask[i];
            ip[i] |= (prefix[i] & mask[i]);
        }

        // if (ip.length == 16) {
        // // We always allocate a /112 for IPv6
        // ip[14] = 0;
        // ip[15] = 0;
        // }

        InetAddress address = NetworkPools.toAddress(ip);
        return address;
    }

    @Override
    public NetworkPoolAllocation markIpAllocated(Project project, InetAddress ip) throws CloudException {
        Set<String> reserved = getAddressesInUse();
        if (reserved.contains(InetAddresses.toAddrString(ip))) {
            return null;
        }

        NetworkPoolAllocation created = markIpAllocated0(project, ip);
        return created;
    }
}
