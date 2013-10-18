package io.fathom.cloud.compute.actions.network;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.compute.networks.VirtualIp;
import io.fathom.cloud.compute.scheduler.SchedulerHost;
import io.fathom.cloud.protobuf.CloudModel.InstanceData;

public class Layer3VirtualIpMapper extends VirtualIpMapper {

    @Override
    public String mapIp(SchedulerHost host, InstanceData instance, VirtualIp vip) throws CloudException {
        // No-op
        // TODO: Arp?

        return vip.getData().getIp();
    }

    @Override
    public void unmapIp(SchedulerHost host, InstanceData instance, VirtualIp vip) throws CloudException {
        // No-op
        // TODO: Arp?
    }

}
