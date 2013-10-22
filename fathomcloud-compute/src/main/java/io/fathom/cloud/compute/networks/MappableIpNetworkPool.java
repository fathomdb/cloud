package io.fathom.cloud.compute.networks;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.protobuf.CloudModel.VirtualIpData;
import io.fathom.cloud.protobuf.CloudModel.VirtualIpPoolData;
import io.fathom.cloud.server.model.Project;

import java.net.InetAddress;

import com.google.common.net.InetAddresses;

/**
 * A pool of IP addresses that can be remapped, like IPs on a common Layer 2
 * network, or EC2 Elastic IP, or OpenStack Floating IPs. This is used to
 * implement Floating IPs. Compare with {@link HostNetworkPool}
 * 
 */
public class MappableIpNetworkPool extends NetworkPoolBase {

    public class Allocation extends NetworkPoolAllocation {
        final VirtualIpData data;

        public Allocation(VirtualIpData data) {
            this.data = data;
        }

        public VirtualIpData getData() {
            return data;
        }

        @Override
        public void releaseIp() throws CloudException {
            MappableIpNetworkPool.this.releaseIp(data);
        }

        @Override
        public InetAddress getAddress() {
            return InetAddresses.forString(data.getIp());
        }

        public VirtualIp getVirtualIp() {
            return new VirtualIp(poolData, data);
        }
    }

    protected final VirtualIpPoolData poolData;
    private final NetworkPools networkPools;
    private final AddressPool addressPool;

    MappableIpNetworkPool(NetworkPools networkPools, VirtualIpPoolData data) {
        this.networkPools = networkPools;
        this.poolData = data;

        this.addressPool = new AddressPool();
        for (String cidr : data.getCidrList()) {
            IpRange ipRange = IpRange.parse(cidr);
            this.addressPool.add(ipRange, getExclusions(ipRange));
        }
    }

    @Override
    public String getNetworkKey() {
        return "_vip_" + poolData.getId();
    }

    public boolean isPublicNetwork() {
        return true;
    }

    @Override
    protected Allocation reserveIp0(Project project, InetAddress ip) throws CloudException {
        VirtualIpData.Builder addr = VirtualIpData.newBuilder();
        addr.setIp(InetAddresses.toAddrString(ip));
        addr.setProjectId(project.getId());

        VirtualIpData allocated = networkPools.networkStateStore.reserveIp(this, addr);
        return new Allocation(allocated);
    }

    public long getPoolId() {
        return poolData.getId();
    }

    @Override
    protected InetAddress getGateway() {
        return null;
    }

    @Override
    protected boolean isReserved(InetAddress ip) throws CloudException {
        for (VirtualIpData i : networkPools.repository.getAllocatedVips(poolData.getId()).list()) {
            InetAddress addr = InetAddresses.forString(i.getIp());
            if (addr.equals(ip)) {
                return true;
            }
        }
        return false;
    }

    // @Override
    // protected List<InetAddress> getAllocatedIps() throws CloudException {
    // List<InetAddress> ret = Lists.newArrayList();
    // for (VirtualIpData i :
    // networkPools.repository.getAllocatedVips(poolData.getId()).list()) {
    // ret.add(InetAddresses.forString(i.getIp()));
    // }
    // return ret;
    // }

    @Override
    public void releaseIpReservation(VirtualIp vip) throws CloudException {
        releaseIp(vip.getData());
    }

    public void releaseIp(VirtualIpData data) throws CloudException {
        networkPools.networkStateStore.releaseIpReservation(MappableIpNetworkPool.this, data);
    }

    @Override
    public AddressPool getAddressPool() {
        return addressPool;
    }

}
