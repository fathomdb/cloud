package io.fathom.cloud.mq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathomdb.TimeSpan;

public class JobRunner implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(JobRunner.class);

    final MessageQueueReader queue;
    final RequestExecutor executor;

    public JobRunner(MessageQueueReader queue, RequestExecutor executor) {
        this.queue = queue;
        this.executor = executor;
    }

    @Override
    public void run() {
        while (true) {
            try {
                byte[] request = queue.poll();

                // TODO: Run in parallel??
                if (request != null) {
                    try {
                        executor.execute(request);
                        continue;
                    } catch (Throwable t) {
                        log.error("Error while running queued message", t);
                    }
                }
            } catch (Throwable e) {
                log.warn("Unexpected exception in job-runner", e);
            }

            TimeSpan.FIVE_SECONDS.doSafeSleep();
        }
    }

    public Thread start() {
        Thread thread = new Thread(this);
        thread.start();
        return thread;
    }
}
