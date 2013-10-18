package io.fathom.cloud.tasks;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathomdb.TimeSpan;
import com.google.inject.Injector;

@Singleton
public class SimpleSchedulerService implements TaskScheduler {
    private static final Logger log = LoggerFactory.getLogger(SimpleSchedulerService.class);

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final ExecutorService threadPool = Executors.newCachedThreadPool();

    @Inject
    Injector injector;

    @Override
    public void schedule(final Runnable runnable, TimeSpan delay) {
        schedule(runnable, delay.getTotalMilliseconds(), TimeUnit.MILLISECONDS);
    }

    @Override
    public ScheduledFuture<?> schedule(final Runnable command, long delay, TimeUnit timeUnit) {
        ScheduledFuture<?> future = scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    threadPool.execute(command);
                } catch (Exception e) {
                    log.warn("Error executing job on thread pool", e);
                }
            }
        }, delay, timeUnit);

        return future;
    }

    class RepeatingTask implements SchedulerTask {
        final Runnable runAndReschedule;

        boolean cancelled;

        public RepeatingTask(final Runnable command, long initialDelay, final long delay, final TimeUnit timeUnit) {
            runAndReschedule = new Runnable() {
                @Override
                public void run() {
                    try {
                        command.run();
                    } catch (Exception e) {
                        log.warn("Error executing job on thread pool", e);
                    }

                    synchronized (this) {
                        if (cancelled) {
                            return;
                        }
                    }

                    try {
                        schedule(this, delay, timeUnit);
                    } catch (Throwable t) {
                        log.error("Error rescheduling fetcher", t);
                    }
                }
            };
        }

        @Override
        public void cancel(boolean mayInterruptIfRunning) {
            synchronized (this) {
                cancelled = true;
            }
        }
    }

    @Override
    public SchedulerTask scheduleWithFixedDelay(final Runnable command, long initialDelay, final long delay,
            final TimeUnit timeUnit) {
        RepeatingTask task = new RepeatingTask(command, initialDelay, delay, timeUnit);
        schedule(task.runAndReschedule, initialDelay, timeUnit);
        return task;
    }

    @Override
    public void schedule(Class<? extends ScheduledTask> clazz) {
        ScheduledTask instance = injector.getInstance(clazz);
        instance.schedule(this);
    }

    @Override
    public Executor getExecutor() {
        return threadPool;
    }

    @Override
    public void execute(final Callable<?> job) {
        // TODO: Need to implement real job queuing/retry
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
                    schedule(new Runnable() {
                        @Override
                        public void run() {
                            execute(job);
                        }
                    }, TimeSpan.FIVE_SECONDS);
                } catch (Exception e) {
                    log.error("Error rescheduling job", e);
                }
            }
        };
        getExecutor().execute(runJob);
    }
}
