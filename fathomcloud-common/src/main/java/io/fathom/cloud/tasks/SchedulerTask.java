package io.fathom.cloud.tasks;

public interface SchedulerTask {
    void cancel(boolean mayInterruptIfRunning);
}
