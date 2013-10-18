package io.fathom.cloud.compute.state;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.compute.networks.HostNetworkPool;
import io.fathom.cloud.compute.networks.MappableIpNetworkPool;
import io.fathom.cloud.protobuf.CloudModel.NetworkAddressData;
import io.fathom.cloud.protobuf.CloudModel.VirtualIpData;
import io.fathom.cloud.state.DuplicateValueException;
import io.fathom.cloud.state.RepositoryBase;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class NetworkStateStore extends RepositoryBase {

    private static final Logger log = LoggerFactory.getLogger(NetworkStateStore.class);

    @Inject
    ComputeRepository respository;

    public NetworkAddressData markIpAllocated(HostNetworkPool pool, NetworkAddressData.Builder builder)
            throws CloudException {
        try {
            return respository.getHostIps(pool.getHostId(), pool.getNetworkKey()).create(builder);
        } catch (DuplicateValueException e) {
            log.info("Duplicate ip allocation blocked: {}", builder.getIp());
            return null;
        }
    }

    public VirtualIpData markIpAllocated(MappableIpNetworkPool pool, VirtualIpData.Builder builder) throws CloudException {
        try {
            return respository.getAllocatedVips(pool.getPoolId()).create(builder);
        } catch (DuplicateValueException e) {
            log.info("Duplicate ip allocation blocked: {}", builder.getIp());
            return null;
        }
    }

    public void releaseIp(HostNetworkPool pool, NetworkAddressData address) throws CloudException {
        respository.getHostIps(pool.getHostId(), pool.getNetworkKey()).delete(address.getIp());
    }

    public void releaseIp(MappableIpNetworkPool pool, VirtualIpData address) throws CloudException {
        respository.getAllocatedVips(pool.getPoolId()).delete(address.getIp());
    }

}
