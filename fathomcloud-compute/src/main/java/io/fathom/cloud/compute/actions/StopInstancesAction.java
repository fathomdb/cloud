package io.fathom.cloud.compute.actions;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.compute.scheduler.InstanceScheduler;
import io.fathom.cloud.compute.services.AsyncTasks;
import io.fathom.cloud.protobuf.CloudModel.InstanceData;
import io.fathom.cloud.server.model.Project;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StopInstancesAction extends Action {
    private static final Logger log = LoggerFactory.getLogger(StopInstancesAction.class);

    @Inject
    InstanceScheduler scheduler;

    @Inject
    AsyncTasks asyncTasks;

    public List<InstanceData> instances;

    public Project project;

    public void go() throws CloudException {
        asyncTasks.stopInstances(instances);
    }
}
