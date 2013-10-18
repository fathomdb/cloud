package io.fathom.cloud.compute.services;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.compute.scheduler.InstanceScheduler;
import io.fathom.cloud.tasks.TaskScheduler;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ComputeServiceImpl implements ComputeService {
    private static final Logger log = LoggerFactory.getLogger(ComputeServiceImpl.class);

    @Inject
    InstanceScheduler scheduler;

    @Inject
    TaskScheduler taskScheduler;

    @Override
    public void start() throws CloudException {
        scheduler.refreshHosts();

        // TODO: Just support method annotations??
        taskScheduler.schedule(PurgeDeletedInstancesTask.class);
    }

    @Override
    public void purgeDeletedInstances() {

    }
}
