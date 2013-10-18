package io.fathom.cloud.dns.backend.aws;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class AwsExecutor {

    private static final Logger log = LoggerFactory.getLogger(AwsExecutor.class);

    private final ScheduledExecutorService executor;

    AwsExecutor() {
        // We run a single AWS task concurrently
        this.executor = Executors.newScheduledThreadPool(1);
    }

    public void execute(final Callable<?> job) {
        // TODO: Refactor / combine with logic from SimpleSchedulerService
        log.warn("Need to implement job queuing/retry");
        final Runnable runJob = new Runnable() {
            @Override
            public void run() {
                try {
                    job.call();
                    return;
                } catch (Exception e) {
                    log.error("Error running job", e);
                }

                log.warn("Job re-scheduling is primitive");

                try {
                    executor.schedule(new Runnable() {
                        @Override
                        public void run() {
                            execute(job);
                        }
                    }, 5, TimeUnit.SECONDS);
                } catch (Exception e) {
                    log.error("Error rescheduling job", e);
                }
            }
        };
        // We put a delay here so that we don't immediately start executing
        // during a batch update
        executor.schedule(runJob, 1, TimeUnit.SECONDS);
    }
}
