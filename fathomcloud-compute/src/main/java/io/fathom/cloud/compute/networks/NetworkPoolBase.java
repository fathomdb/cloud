package io.fathom.cloud.compute.networks;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.server.model.Project;

import java.net.InetAddress;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

public abstract class NetworkPoolBase implements NetworkPool {

    private static final Logger log = LoggerFactory.getLogger(NetworkPoolBase.class);

    public abstract String getNetworkKey();

    protected abstract NetworkPoolAllocation reserveIp0(Project project, InetAddress ip) throws CloudException;

    protected abstract InetAddress getGateway();

    protected abstract AddressPool getAddressPool();

    @Override
    public InetAddress checkIpAvailable(long seed) throws CloudException {
        AddressPool addressPool = getAddressPool();

        InetAddress address = addressPool.convertSeedToIp(seed);
        if (address == null) {
            return null;
        }

        if (isReserved(address)) {
            log.info("Chose system-excluded address; forcing retry");
            return null;
        }

        return address;
    }

    protected List<InetAddress> getExclusions(IpRange ipRange) {
        // Check for single IPs; we assume if we only have one IP that it's not
        // excluded e.g. EC2 floating ips
        if (ipRange.isIpv4() && ipRange.getNetmaskLength() == 32) {
            return Collections.emptyList();
        } else if (ipRange.isIpv6() && ipRange.getNetmaskLength() == 128) {
            return Collections.emptyList();
        }

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

    @Override
    public NetworkPoolAllocation reserveIp(Project project, InetAddress ip) throws CloudException {
        if (isReserved(ip)) {
            return null;
        }

        NetworkPoolAllocation created = reserveIp0(project, ip);
        return created;
    }

    protected abstract boolean isReserved(InetAddress ip) throws CloudException;
}
