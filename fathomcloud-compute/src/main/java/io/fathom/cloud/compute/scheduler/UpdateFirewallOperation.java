package io.fathom.cloud.compute.scheduler;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.compute.services.SecurityGroups;
import io.fathom.cloud.protobuf.CloudModel.InstanceData;
import io.fathom.cloud.protobuf.CloudModel.SecurityGroupData;
import io.fathom.cloud.server.model.Project;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateFirewallOperation extends SchedulerOperation {
    private static final Logger log = LoggerFactory.getLogger(UpdateFirewallOperation.class);

    @Inject
    SecurityGroups securityGroups;

    InstanceData instance;

    private Project project;

    private long hostId;

    private List<SecurityGroupData> securityGroupList;

    public void init(Project project, List<SecurityGroupData> securityGroupList, long hostId) {
        this.project = project;
        this.instance = null;
        this.hostId = hostId;
        this.securityGroupList = securityGroupList;
    }

    public void init(Project project, InstanceData instance) throws CloudException {
        this.project = project;
        this.instance = instance;
        this.hostId = instance.getHostId();
        this.securityGroupList = securityGroups.getSecurityGroups(project, instance);
    }

    @Override
    public boolean run() throws CloudException, IOException {
        SchedulerHost host = scheduler.findHost(hostId);
        if (host == null) {
            throw new IllegalStateException();
        }

        try (ConfigurationOperation config = host.startConfiguration()) {
            config.configureFirewall(instance, securityGroupList);
            config.applyChanges();
        }

        return true;
    }
}
