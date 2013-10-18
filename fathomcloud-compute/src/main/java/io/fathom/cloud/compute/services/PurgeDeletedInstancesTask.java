package io.fathom.cloud.compute.services;

import io.fathom.cloud.tasks.ScheduledTask;

import javax.inject.Inject;

import com.fathomdb.TimeSpan;

public class PurgeDeletedInstancesTask extends ScheduledTask {

    @Inject
    ComputeService computeService;

    @Override
    protected TimeSpan getInterval() {
        return TimeSpan.ONE_HOUR;
    }

    @Override
    public void run() throws Exception {
        computeService.purgeDeletedInstances();
    }
}
