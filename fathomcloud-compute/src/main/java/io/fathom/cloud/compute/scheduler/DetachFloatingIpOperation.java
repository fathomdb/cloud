package io.fathom.cloud.compute.scheduler;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.compute.networks.VirtualIp;
import io.fathom.cloud.compute.services.ComputeServices;
import io.fathom.cloud.compute.services.IpPools;
import io.fathom.cloud.protobuf.CloudModel.InstanceData;
import io.fathom.cloud.server.model.Project;

import java.io.IOException;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DetachFloatingIpOperation extends SchedulerOperation {
    private static final Logger log = LoggerFactory.getLogger(DetachFloatingIpOperation.class);

    @Inject
    InstanceScheduler scheduler;

    @Inject
    ComputeServices computeServices;

    @Inject
    IpPools ipPools;

    public Project project;

    String ip;
    long instanceId;

    public void init(Project project, InstanceData instance, VirtualIp vip) {
        this.project = project;
        this.instanceId = instance.getId();
        this.ip = vip.getData().getIp();
    }

    @Override
    public boolean run() throws CloudException, IOException {
        VirtualIp vip = ipPools.findVirtualIp(project, ip);
        if (vip == null) {
            log.warn("Virtual IP not found, giving up: {}", ip);
            throw new IllegalStateException();
        }

        if (vip.getData().hasInstanceId()) {
            log.warn("Virtual IP attached to new machine: {}", ip);
            throw new IllegalStateException();
        }

        InstanceData instance = computeServices.findInstance(project.getId(), instanceId);
        if (instance == null) {
            log.warn("Instance not found, giving up: {}", instanceId);
            return false;
        }

        SchedulerHost host = scheduler.findHost(instance.getHostId());
        if (host == null) {
            throw new IllegalStateException();
        }

        try (ConfigurationOperation config = host.startConfiguration()) {
            config.detachVip(instance, vip);
            config.applyChanges();
        }

        // TODO: Mark vip as detached??

        return true;
    }

}
