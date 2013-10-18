/*
 * Copyright 2012 Netflix, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package io.fathom.auto.locks;

import io.fathom.auto.TimeSpan;
import io.fathom.auto.config.Hostname;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

public abstract class PseudoLockBase implements Lock {
    private static final Logger log = LoggerFactory.getLogger(PseudoLockBase.class);

    private final long timeoutMs;
    private final long pollingMs;
    private final long settlingMs;

    // all guarded by sync
    private long lastUpdateMs = 0;
    private boolean ownsTheLock;
    private String key;
    private long lockStartMs = 0;

    private boolean createdFile;

    private static final String content = Hostname.getHostname();

    private static final Random random = new SecureRandom();

    private static final String SEPARATOR = "_";
    private static final TimeSpan DEFAULT_SETTLING_MS = TimeSpan.seconds(5);

    private static final int MISSING_KEY_FACTOR = 10;

    /**
     * @param lockPrefix
     *            key prefix
     * @param timeoutMs
     *            max age for locks
     * @param pollingMs
     *            how often to poll S3
     */
    public PseudoLockBase(TimeSpan timeoutMs, TimeSpan pollingMs) {
        this(timeoutMs, pollingMs, DEFAULT_SETTLING_MS);
    }

    /**
     * @param lockPrefix
     *            key prefix
     * @param timeoutMs
     *            max age for locks
     * @param pollingMs
     *            how often to poll S3
     * @param settlingMs
     *            how long to wait for S3 to reach consistency
     */
    public PseudoLockBase(TimeSpan timeoutMs, TimeSpan pollingMs, TimeSpan settlingMs) {
        this.settlingMs = settlingMs.toMillis();

        this.pollingMs = pollingMs.toMillis();
        this.timeoutMs = timeoutMs.toMillis();
    }

    /**
     * Acquire the lock, blocking at most <code>maxWait</code> until it is
     * acquired
     * 
     * 
     * @param log
     *            the logger
     * @param maxWait
     *            max time to wait
     * @param unit
     *            time unit
     * @return true if the lock was acquired
     * @throws InterruptedException
     * @throws Exception
     *             errors
     */
    @Override
    public synchronized boolean tryLock(long maxWait, TimeUnit unit) throws InterruptedException {
        if (ownsTheLock) {
            throw new IllegalStateException("Already locked");
        }

        log.debug("Trying to obtain lock: {}", this);

        lockStartMs = System.currentTimeMillis();

        key = newRandomSequence();

        long startMs = System.currentTimeMillis();
        boolean hasMaxWait = (unit != null);
        long maxWaitMs = hasMaxWait ? TimeUnit.MILLISECONDS.convert(maxWait, unit) : Long.MAX_VALUE;
        Preconditions.checkState(maxWaitMs >= settlingMs,
                String.format("The maxWait ms (%d) is less than the settling ms (%d)", maxWaitMs, settlingMs));

        try {
            createFile(key, content);
            createdFile = true;
        } catch (IOException e) {
            throw new RuntimeException("Error creating lock file", e);
        }

        for (;;) {
            try {
                checkUpdate();
            } catch (IOException e) {
                throw new RuntimeException("Error checking lock file status", e);
            }
            if (ownsTheLock) {
                break;
            }
            long thisWaitMs;
            if (hasMaxWait) {
                long elapsedMs = System.currentTimeMillis() - startMs;
                thisWaitMs = maxWaitMs - elapsedMs;
                if (thisWaitMs <= 0) {
                    log.error(String.format("Could not acquire lock within %d ms, polling: %d ms, key: %s", maxWaitMs,
                            pollingMs, key));
                    break;
                }
            } else {
                thisWaitMs = pollingMs;
            }
            wait(Math.min(pollingMs, thisWaitMs));
        }
        return ownsTheLock;
    }

    /**
     * Release the lock
     * 
     * @throws Exception
     *             errors
     */
    @Override
    public synchronized void unlock() {
        if (createdFile) {
            try {
                deleteFile(key);
            } catch (IOException e) {
                throw new RuntimeException("Error releasing lock", e);
            }
            createdFile = false;
        }
        notifyAll();
        ownsTheLock = false;
    }

    protected abstract void createFile(String key, String content) throws IOException;

    protected abstract void deleteFile(String key) throws IOException;

    protected abstract List<String> getFileNames() throws IOException;

    private synchronized void checkUpdate() throws IOException {
        if ((System.currentTimeMillis() - lastUpdateMs) < pollingMs) {
            return;
        }

        List<String> keys = getFileNames();
        log.debug(String.format("keys: %s", keys));
        keys = cleanOldObjects(keys);
        log.debug(String.format("cleaned keys: %s", keys));
        Collections.sort(keys);

        if (keys.size() > 0) {
            String lockerKey = keys.get(0);
            long lockerAge = System.currentTimeMillis() - getEpochStampForKey(key);
            ownsTheLock = false;
            if (lockerKey.equals(key)) {
                if (lockerAge >= settlingMs) {
                    ownsTheLock = true;
                } else {
                    log.debug("Lock match, but waiting to settle: {} vs {}", lockerAge, settlingMs);
                }
            }
        } else {
            long elapsed = System.currentTimeMillis() - lockStartMs;
            if (elapsed > (settlingMs * MISSING_KEY_FACTOR)) {
                throw new IOException(String.format("Our key is missing. Key: %s, Elapsed: %d, Max Wait: %d", key,
                        elapsed, settlingMs * MISSING_KEY_FACTOR));
            }
        }

        lastUpdateMs = System.currentTimeMillis();

        notifyAll();
    }

    private List<String> cleanOldObjects(List<String> keys) throws IOException {
        List<String> newKeys = Lists.newArrayList();
        for (String key : keys) {
            long epochStamp = getEpochStampForKey(key);
            if (!key.equals(this.key) && ((System.currentTimeMillis() - epochStamp) > timeoutMs)) {
                deleteFile(key);
            } else {
                newKeys.add(key);
            }
        }
        return newKeys;
    }

    private static long getEpochStampForKey(String key) {
        String[] parts = key.split(SEPARATOR);
        long millisecondStamp = 0;
        try {
            millisecondStamp = Long.parseLong(parts[0]);
        } catch (NumberFormatException ignore) {
            // ignore
            log.warn("Error parsing epoch stamp for key: " + key);
        }
        return millisecondStamp;
    }

    private String newRandomSequence() {
        return "" + System.currentTimeMillis() + SEPARATOR + Math.abs(random.nextLong());
    }

    @Override
    public void lock() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean tryLock() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Condition newCondition() {
        throw new UnsupportedOperationException();
    }
}
