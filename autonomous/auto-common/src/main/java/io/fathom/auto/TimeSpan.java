package io.fathom.auto;

import java.util.concurrent.TimeUnit;

public class TimeSpan {
    final long duration;
    final TimeUnit unit;

    public TimeSpan(long duration, TimeUnit unit) {
        this.duration = duration;
        this.unit = unit;
    }

    public long toMillis() {
        return unit.toMillis(duration);
    }

    public static TimeSpan minutes(long duration) {
        return new TimeSpan(duration, TimeUnit.MINUTES);
    }

    public static TimeSpan seconds(long duration) {
        return new TimeSpan(duration, TimeUnit.SECONDS);
    }

    public void sleep() /* throws InterruptedRuntimeException */{
        try {
            Thread.sleep(toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InterruptedError(e);
        }
    }

    public void join(Thread thread) {
        try {
            thread.join(toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InterruptedError(e);
        }
    }
}
