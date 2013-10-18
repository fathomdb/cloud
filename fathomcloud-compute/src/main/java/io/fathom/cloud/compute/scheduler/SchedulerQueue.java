package io.fathom.cloud.compute.scheduler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SchedulerQueue {

    private static final Logger log = LoggerFactory.getLogger(SchedulerQueue.class);

    final ExecutorService executorService = Executors.newFixedThreadPool(20);

    public void add(final SchedulerOperation operation) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    log.debug("Starting scheduler operation: {}", operation);
                    operation.run();
                } catch (Throwable t) {
                    log.warn("Error while running queued operation", t);
                }
            }
        });
    }

}
