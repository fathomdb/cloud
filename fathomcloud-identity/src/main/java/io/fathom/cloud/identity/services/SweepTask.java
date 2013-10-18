package io.fathom.cloud.identity.services;

import io.fathom.cloud.tasks.ScheduledTask;

import javax.inject.Inject;

import com.fathomdb.TimeSpan;

public class SweepTask extends ScheduledTask {

    @Inject
    IdentityService identityService;

    @Override
    protected TimeSpan getInterval() {
        return TimeSpan.FIFTEEN_MINUTES;
    }

    @Override
    public void run() throws Exception {
        identityService.sweep();
    }
}
