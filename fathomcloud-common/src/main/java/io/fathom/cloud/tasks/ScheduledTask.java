package io.fathom.cloud.tasks;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathomdb.TimeSpan;

public abstract class ScheduledTask {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTask.class);

    public void schedule(TaskScheduler scheduler) {
        TimeSpan initialDelay = getInitialDelay();
        TimeSpan interval = getInterval();

        scheduler.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                try {
                    log.info("Running scheduled task: {}", getKey());
                    ScheduledTask.this.run();
                } catch (Exception e) {
                    log.error("Error running scheduled task: " + getKey(), e);
                }
            }
        }, initialDelay.getTotalMilliseconds(), interval.getTotalMilliseconds(), TimeUnit.MILLISECONDS);
    }

    protected String getKey() {
        return getClass().getSimpleName();
    }

    protected abstract TimeSpan getInterval();

    protected TimeSpan getInitialDelay() {
        return getInterval();
    }

    public abstract void run() throws Exception;
}
