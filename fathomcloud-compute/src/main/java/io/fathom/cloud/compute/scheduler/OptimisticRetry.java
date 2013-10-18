package io.fathom.cloud.compute.scheduler;

import io.fathom.cloud.CloudException;

import java.util.concurrent.Callable;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.OptimisticLockException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.persist.UnitOfWork;

@Singleton
@Deprecated
public class OptimisticRetry {
    private static final Logger log = LoggerFactory.getLogger(OptimisticRetry.class);

    @Inject
    UnitOfWork unitOfWork;

    public <T> T run(Callable<T> runnable) throws CloudException {
        int attempt = 0;

        while (true) {
            attempt++;

            unitOfWork.begin();
            try {
                T t = runnable.call();
                return t;
            } catch (OptimisticLockException e) {
                log.warn("Retrying update after OptimisticLockException", e);
                continue;
            } catch (Exception e) {
                throw new CloudException("Error performing update", e);
            } finally {
                unitOfWork.end();
            }
        }
    }
}
