package io.fathom.cloud.compute.api.aws.ec2.actions;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.compute.api.aws.ec2.model.InstanceStateChange;
import io.fathom.cloud.compute.api.aws.ec2.model.TerminateInstancesResponse;
import io.fathom.cloud.compute.scheduler.InstanceScheduler;
import io.fathom.cloud.compute.services.AsyncTasks;
import io.fathom.cloud.protobuf.CloudModel;
import io.fathom.cloud.protobuf.CloudModel.InstanceData;
import io.fathom.cloud.server.model.Project;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

@AwsAction("TerminateInstances")
public class TerminateInstances extends AwsActionHandler {
    private static final Logger log = LoggerFactory.getLogger(TerminateInstances.class);
    @Inject
    InstanceScheduler scheduler;

    @Inject
    AsyncTasks asyncTasks;

    @Override
    public Object go() throws CloudException {
        // UserData user = getUser();
        Project project = getProject();

        List<String> instanceEc2Ids = getList("InstanceId");

        TerminateInstancesResponse response = new TerminateInstancesResponse();
        response.requestId = getRequestId();

        response.instances = Lists.newArrayList();

        List<InstanceData> stopInstances = Lists.newArrayList();

        for (String instanceEc2Id : instanceEc2Ids) {
            long instanceId = decodeEc2Id("i-", instanceEc2Id);
            InstanceData instance = instanceStateStore.getInstances(getProject().getId()).find(instanceId);
            if (instance == null) {
                throw new CloudException("The instance ID '" + instanceEc2Id + "' does not exist");
            }
            stopInstances.add(instance);
        }

        asyncTasks.stopInstances(stopInstances);
        for (InstanceData instance : stopInstances) {
            // This is a very abbreviated state
            InstanceStateChange instanceStateChange = new InstanceStateChange();
            instanceStateChange.instanceId = toEc2InstanceId(instance.getId());

            instanceStateChange.currentState = buildInstanceState(instance);
            instanceStateChange.previousState = buildInstanceState(CloudModel.InstanceState.STOPPING);

            response.instances.add(instanceStateChange);
        }

        return response;
    }

}
