package io.fathom.cloud.compute.services;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.compute.api.os.model.actions.AddFloatingIpRequest;
import io.fathom.cloud.compute.api.os.model.actions.RemoveFloatingIpRequest;
import io.fathom.cloud.compute.networks.MappableIpNetworkPool;
import io.fathom.cloud.compute.networks.NetworkPool;
import io.fathom.cloud.compute.networks.NetworkPoolAllocation;
import io.fathom.cloud.compute.networks.NetworkPools;
import io.fathom.cloud.compute.networks.VirtualIp;
import io.fathom.cloud.compute.state.ComputeRepository;
import io.fathom.cloud.protobuf.CloudModel.InstanceData;
import io.fathom.cloud.protobuf.CloudModel.VirtualIpData;
import io.fathom.cloud.protobuf.CloudModel.VirtualIpPoolData;
import io.fathom.cloud.protobuf.CloudModel.VirtualIpPoolData.Builder;
import io.fathom.cloud.protobuf.CloudModel.VirtualIpPoolType;
import io.fathom.cloud.server.model.Project;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.inject.persist.Transactional;

@Transactional
@Singleton
public class IpPools {
    private static final Logger log = LoggerFactory.getLogger(IpPools.class);

    @Inject
    ComputeRepository computeRepository;

    @Inject
    NetworkPools networkPools;

    @Inject
    AsyncTasks asyncTasks;

    public VirtualIp findVirtualIp(Project project, String address) throws CloudException {
        for (VirtualIpPoolData pool : computeRepository.getVirtualIpPools().list()) {
            VirtualIpData vip = computeRepository.getAllocatedVips(pool.getId()).find(address);
            if (vip == null) {
                continue;
            }

            if (vip.getProjectId() != project.getId()) {
                continue;
            }

            return new VirtualIp(pool, vip);
        }
        return null;
    }

    // public VirtualIp findVirtualIp(Project project, long id) throws
    // CloudException {
    // for (VirtualIpPoolData pool :
    // computeRepository.getVirtualIpPools().list()) {
    // for (VirtualIpData vip :
    // computeRepository.getAllocatedVips(pool.getId()).list()) {
    // if (vip.getProjectId() != project.getId()) {
    // continue;
    // }
    // if (id == getId(vip)) {
    // return new VirtualIp(pool, vip);
    // }
    // }
    // }
    // return null;
    // }

    // public static String getId(VirtualIpData vip) {
    // return vip.getIp();
    // //
    // // InetAddress address = InetAddresses.forString(vip.getIp());
    // // if (address instanceof Inet4Address) {
    // // Inet4Address inet4 = (Inet4Address) address;
    // // byte[] b = inet4.getAddress();
    // // int i = ((b[0] & 0xFF) << 24) | ((b[1] & 0xFF) << 16) | ((b[2] &
    // // 0xFF) << 8) | ((b[3] & 0xFF) << 0);
    // // return i;
    // // } else {
    // // throw new UnsupportedOperationException();
    // // }
    // }

    public List<VirtualIp> listVirtualIps(Project project) throws CloudException {
        List<VirtualIp> ret = Lists.newArrayList();
        for (VirtualIpPoolData pool : computeRepository.getVirtualIpPools().list()) {
            for (VirtualIpData vip : computeRepository.getAllocatedVips(pool.getId()).list()) {
                if (vip.getProjectId() != project.getId()) {
                    continue;
                }
                ret.add(new VirtualIp(pool, vip));
            }
        }
        return ret;
    }

    @Transactional
    public void attachFloatingIp(Project project, InstanceData instance, AddFloatingIpRequest request)
            throws CloudException {
        String address = request.address;

        if (address == null) {
            // TODO: Auto-allocate a suitable floating ip from the networks the
            // instance can see
            throw new IllegalArgumentException();
        }
        VirtualIp vip = findVirtualIp(project, address);
        if (vip == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        if (vip.getData().hasInstanceId()) {
            throw new WebApplicationException(Status.CONFLICT);
        }

        VirtualIpData.Builder b = VirtualIpData.newBuilder(vip.getData());
        b.setInstanceId(instance.getId());
        VirtualIpData updated = computeRepository.getAllocatedVips(vip.getPoolData().getId()).update(b);

        asyncTasks.attachFloatingIp(project, instance, new VirtualIp(vip.getPoolData(), updated));
    }

    public void detachFloatingIp(Project project, InstanceData instance, RemoveFloatingIpRequest request)
            throws CloudException {
        String address = request.address;
        if (address == null) {
            throw new IllegalArgumentException();
        }

        VirtualIp vip = findVirtualIp(project, address);
        if (vip == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        VirtualIpData vipData = vip.getData();

        if (!vipData.hasInstanceId() || vipData.getInstanceId() != instance.getId()) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        VirtualIpData.Builder b = VirtualIpData.newBuilder(vip.getData());
        b.clearInstanceId();
        VirtualIpData updated = computeRepository.getAllocatedVips(vip.getPoolData().getId()).update(b);

        asyncTasks.detachFloatingIp(project, instance, new VirtualIp(vip.getPoolData(), updated));
    }

    public VirtualIp allocateFloatingIp(Project project, VirtualIpPoolData poolData) throws CloudException {
        NetworkPool pool = networkPools.buildPool(poolData);

        NetworkPoolAllocation allocation = networkPools.allocateIp(project, pool);

        return ((MappableIpNetworkPool.Allocation) allocation).getVirtualIp();
    }

    public void deallocateFloatingIp(Project project, String ip) throws CloudException {
        VirtualIp vip = findVirtualIp(project, ip);
        if (vip == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        if (vip.getData().hasInstanceId()) {
            // TODO: Auto-detach?
            throw new WebApplicationException(Status.CONFLICT);
        }

        NetworkPool pool = networkPools.buildPool(vip.getPoolData());
        pool.markIpNotAllocated(vip);
    }

    public VirtualIpPoolData findVirtualIpPool(Project project, long poolId) throws CloudException {
        // TODO: Restrict access to pools by project?
        return computeRepository.getVirtualIpPools().find(poolId);
    }

    public List<VirtualIpPoolData> listVirtualIpPools(Project project) throws CloudException {
        // TODO: Restrict access to pools by project?
        return computeRepository.getVirtualIpPools().list();
    }

    public VirtualIpPoolData createVipPool(Builder b) throws CloudException {
        if (b.getType() == VirtualIpPoolType.LAYER_3) {
            if (!b.hasCidr()) {
                throw new IllegalArgumentException();
            }
        }

        if (b.getType() == VirtualIpPoolType.AMAZON_EC2) {
            if (b.hasCidr()) {
                throw new IllegalArgumentException();
            }
        }

        return computeRepository.getVirtualIpPools().create(b);
    }

    public void deleteVirtualIpPool(VirtualIpPoolData pool) throws CloudException {
        computeRepository.getVirtualIpPools().delete(pool.getId());
    }

}
