package io.fathom.cloud.compute.actions.network;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.compute.networks.VirtualIp;
import io.fathom.cloud.compute.scheduler.SchedulerHost;
import io.fathom.cloud.protobuf.CloudModel.InstanceData;
import io.fathom.cloud.protobuf.CloudModel.VirtualIpPoolType;

public abstract class VirtualIpMapper {
    public static VirtualIpMapper build(SchedulerHost host, InstanceData instance, VirtualIp vip) {
        VirtualIpPoolType type = vip.getPoolData().getType();
        switch (type) {
        case AMAZON_EC2:
            return new Ec2VirtualIpMapper();

        case LAYER_3:
            return new Layer3VirtualIpMapper();

        default:
            throw new IllegalStateException();
        }
    }

    /**
     * Maps the public ip to the host.
     * 
     * @return the private ip on the host (different if using NAT)
     * @throws CloudException
     */
    public abstract String mapIp(SchedulerHost host, InstanceData instance, VirtualIp vip) throws CloudException;

    public abstract void unmapIp(SchedulerHost host, InstanceData instance, VirtualIp vip) throws CloudException;
}
