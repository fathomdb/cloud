package io.fathom.cloud.compute.scheduler;

import io.fathom.cloud.CloudException;

import java.io.IOException;

import javax.inject.Inject;

import com.google.inject.persist.UnitOfWork;

public abstract class SchedulerOperation {

    @Inject
    UnitOfWork unitOfWork;

    @Inject
    protected InstanceScheduler scheduler;

    @Inject
    SchedulerServices services;

    public abstract boolean run() throws CloudException, IOException;

}
