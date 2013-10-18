package io.fathom.cloud.compute.services;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.compute.actions.StartInstancesAction.StartInstanceData;
import io.fathom.cloud.compute.networks.VirtualIp;
import io.fathom.cloud.compute.scheduler.AttachFloatingIpOperation;
import io.fathom.cloud.compute.scheduler.DetachFloatingIpOperation;
import io.fathom.cloud.compute.scheduler.SchedulerOperation;
import io.fathom.cloud.compute.scheduler.SchedulerQueue;
import io.fathom.cloud.compute.scheduler.StartInstanceOperation;
import io.fathom.cloud.compute.scheduler.StopInstanceOperation;
import io.fathom.cloud.compute.scheduler.UpdateFirewallOperation;
import io.fathom.cloud.protobuf.CloudModel.InstanceData;
import io.fathom.cloud.protobuf.CloudModel.SecurityGroupData;
import io.fathom.cloud.server.auth.Auth;
import io.fathom.cloud.server.model.Project;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.google.inject.Injector;

@Singleton
public class AsyncTasks {

    @Inject
    Injector injector;

    @Inject
    SchedulerQueue schedulerQueue;

    public void attachFloatingIp(Project project, InstanceData instance, VirtualIp vip) {
        AttachFloatingIpOperation op = injector.getInstance(AttachFloatingIpOperation.class);
        op.init(project, instance, vip);
        enqueue(op);
    }

    public void detachFloatingIp(Project project, InstanceData instance, VirtualIp vip) {
        DetachFloatingIpOperation op = injector.getInstance(DetachFloatingIpOperation.class);
        op.init(project, instance, vip);
        enqueue(op);
    }

    public void updateInstanceSecurityGroups(Project project, InstanceData instance) throws CloudException {
        UpdateFirewallOperation op = injector.getInstance(UpdateFirewallOperation.class);
        op.init(project, instance);
        enqueue(op);
    }

    public void updateSecurityGroupDefinition(Project project, SecurityGroupData securityGroup, long hostId) {
        UpdateFirewallOperation op = injector.getInstance(UpdateFirewallOperation.class);
        op.init(project, Collections.singletonList(securityGroup), hostId);
        enqueue(op);
    }

    @Inject
    Provider<StartInstanceOperation> startOperationProvider;

    public void startInstances(Auth auth, Project project, List<StartInstanceData> starts) {
        for (StartInstanceData start : starts) {
            StartInstanceOperation op = startOperationProvider.get();
            op.init(auth, project, start);
            enqueue(op);
        }
    }

    @Inject
    Provider<StopInstanceOperation> stopOperationProvider;

    public void stopInstances(List<InstanceData> instances) {
        for (InstanceData instance : instances) {
            StopInstanceOperation op = stopOperationProvider.get();
            op.init(instance);
            enqueue(op);
        }
    }

    private void enqueue(SchedulerOperation operation) {
        schedulerQueue.add(operation);
    }
}
