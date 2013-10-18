package io.fathom.cloud.tasks;

public interface Task {
    void schedule(TaskScheduler scheduler);

    void run() throws Exception;
}
