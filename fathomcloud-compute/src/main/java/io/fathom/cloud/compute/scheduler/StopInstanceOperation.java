package io.fathom.cloud.compute.scheduler;

import io.fathom.cloud.Clock;
import io.fathom.cloud.CloudException;
import io.fathom.cloud.protobuf.CloudModel.InstanceData;
import io.fathom.cloud.protobuf.CloudModel.InstanceState;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

public class StopInstanceOperation extends SchedulerOperation {
    private static final Logger log = LoggerFactory.getLogger(StopInstanceOperation.class);

    InstanceData instance;

    public void init(InstanceData instance) {
        this.instance = instance;
    }

    @Override
    public boolean run() throws CloudException, IOException {
        SchedulerHost host = scheduler.findHost(instance.getHostId());
        if (host == null) {
            throw new IllegalStateException();
        }

        String hostCookie = instance.getHostCookie();
        if (!Strings.isNullOrEmpty(hostCookie)) {
            UUID containerId = UUID.fromString(hostCookie);
            host.stopContainer(containerId);
        }

        try (ConfigurationOperation config = host.startConfiguration()) {
            config.removeFirewallConfig(instance);
            config.applyChanges();
        }

        services.updateInstance(instance, InstanceData.newBuilder().setInstanceState(InstanceState.TERMINATED)
                .setTerminatedAt(Clock.getTimestamp()));

        return true;
    }
}
