package io.fathom.cloud.tasks;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.fathomdb.TimeSpan;
import com.google.inject.ImplementedBy;

@ImplementedBy(SimpleSchedulerService.class)
public interface TaskScheduler {
    SchedulerTask scheduleWithFixedDelay(Runnable runnable, long initialDelay, long delay, TimeUnit timeUnit);

    ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit timeUnit);

    void schedule(Runnable command, TimeSpan delay);

    void schedule(Class<? extends ScheduledTask> clazzz);

    Executor getExecutor();

    void execute(Callable<?> job);
}
