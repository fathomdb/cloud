package io.fathom.cloud.compute.networks;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.compute.scheduler.SchedulerHost;
import io.fathom.cloud.compute.scheduler.SchedulerHost.SchedulerHostNetwork;
import io.fathom.cloud.protobuf.CloudModel.InstanceData;
import io.fathom.cloud.protobuf.CloudModel.NetworkAddressData;
import io.fathom.cloud.server.model.Project;

import java.net.InetAddress;

import com.google.common.net.InetAddresses;

/**
 * A pool of ip addresses that are locked to a host, like with most 'simple'
 * dedicated servers or virtual servers. This is currently used for 'normal' IP
 * allocation when an instance starts up and gets private IP addresses. Compare
 * with {@link MappableIpNetworkPool}
 */
public class HostNetworkPool extends NetworkPoolBase {
    public class Allocation extends NetworkPoolAllocation {
        final NetworkAddressData data;

        public Allocation(NetworkAddressData data) {
            this.data = data;
        }

        public NetworkAddressData getData() {
            return data;
        }

        @Override
        public void releaseIp() throws CloudException {
            networkPools.networkStateStore.releaseIpReservation(HostNetworkPool.this, data);
        }

        @Override
        public InetAddress getAddress() {
            return InetAddresses.forString(data.getIp());
        }
    }

    private final SchedulerHost host;
    private final SchedulerHostNetwork network;
    private final NetworkPools networkPools;
    private final InstanceData instance;
    private final AddressPool addressPool;

    HostNetworkPool(NetworkPools networkPools, SchedulerHost host, SchedulerHostNetwork network, InstanceData instance) {
        this.networkPools = networkPools;
        this.host = host;
        this.network = network;
        this.instance = instance;

        this.addressPool = new AddressPool();
        this.addressPool.add(network.getIpRange(), getExclusions(network.getIpRange()));
    }

    public long getHostId() {
        return host.getId();
    }

    @Override
    public String getNetworkKey() {
        return network.getKey();
    }

    @Override
    public InetAddress getGateway() {
        return network.getGateway();
    }

    public boolean isPublicNetwork() {
        return network.isPublicNetwork();
    }

    @Override
    protected NetworkPoolAllocation reserveIp0(Project project, InetAddress ip) throws CloudException {
        IpRange ipRange = network.getIpRange();

        NetworkAddressData.Builder addr = NetworkAddressData.newBuilder();
        addr.setIp(InetAddresses.toAddrString(ip));
        addr.setGateway(InetAddresses.toAddrString(getGateway()));
        addr.setPrefixLength(ipRange.getNetmaskLength());
        addr.setPublicAddress(isPublicNetwork());
        addr.setNetworkKey(getNetworkKey());

        addr.setProjectId(instance.getProjectId());
        addr.setInstanceId(instance.getId());

        NetworkAddressData allocated = networkPools.networkStateStore.reserveIp(this, addr);
        return new Allocation(allocated);
    }

    @Override
    protected boolean isReserved(InetAddress ip) throws CloudException {
        for (NetworkAddressData i : networkPools.repository.getHostIps(host.getId(), getNetworkKey()).list()) {
            InetAddress addr = InetAddresses.forString(i.getIp());
            if (addr.equals(ip)) {
                return true;
            }
        }
        return false;
    }

    // @Override
    // public List<InetAddress> getAllocatedIps() throws CloudException {
    // List<InetAddress> ret = Lists.newArrayList();
    // for (NetworkAddressData i :
    // networkPools.repository.getHostIps(host.getId(), getNetworkKey()).list())
    // {
    // ret.add(InetAddresses.forString(i.getIp()));
    // }
    // return ret;
    // }

    @Override
    public void releaseIpReservation(VirtualIp vip) throws CloudException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected AddressPool getAddressPool() {
        return addressPool;
    }
}
