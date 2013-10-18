package io.fathom.cloud;

import java.util.Date;

public class Clock {
    public static long getTimestamp() {
        long now = System.currentTimeMillis();
        now /= 1000L;

        return now;
    }

    public static Date toDate(long v) {
        if (v == 0) {
            return null;
        }

        Date d = new Date(toMillis(v));
        return d;
    }

    public static long toMillis(long v) {
        v *= 1000L;
        return v;
    }

}
