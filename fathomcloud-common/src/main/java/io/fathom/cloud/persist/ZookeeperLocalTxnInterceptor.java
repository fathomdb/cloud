package io.fathom.cloud.persist;

import java.lang.reflect.Method;

import javax.persistence.EntityTransaction;
import javax.persistence.OptimisticLockException;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import com.google.inject.persist.UnitOfWork;

/**
 * Based on JpaLocalTxnInterceptor
 */
class ZookeeperLocalTxnInterceptor implements MethodInterceptor {
    private static final Logger log = LoggerFactory.getLogger(ZookeeperLocalTxnInterceptor.class);

    @Inject
    private final ZookeeperPersistService emProvider = null;

    @Inject
    private final UnitOfWork unitOfWork = null;

    @Transactional
    private static class Internal {
    }

    // TODO: In Guice, this is ThreadLocal. Not clear why!
    // Tracks if the unit of work was begun implicitly by this transaction.
    // private final ThreadLocal<Boolean> didWeStartWork = new
    // ThreadLocal<Boolean>();

    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        int attempt = 0;

        int maxAttempts = maxAttempts();

        while (true) {
            attempt++;

            try {
                boolean didWeStartWork = false;

                // Should we start a unit of work?
                if (!emProvider.isWorking()) {
                    emProvider.begin();
                    didWeStartWork = true;
                }

                ZookeeperEntityManager em = this.emProvider.get();

                // Allow 'joining' of transactions if there is an enclosing
                // @Transactional method.
                if (em.getTransaction().isActive()) {
                    // Don't retry at this level; rely on the outer transaction
                    maxAttempts = 0;

                    return methodInvocation.proceed();
                }

                return invoke0(methodInvocation, em, didWeStartWork);
            } catch (OptimisticLockException e) {
                boolean retry = false;

                if (attempt < maxAttempts) {
                    retry = true;
                }

                if (!retry) {
                    // throw new CloudException(
                    // "Unable to update due to concurrent modification",
                    // e);

                    if (maxAttempts != 0) {
                        log.warn("Too many retries on OptimisticLockException", e);
                    }

                    throw e;
                } else {
                    log.warn("Retrying after OptimisticLockException");
                    continue;
                }
            }
        }
    }

    private Object invoke0(MethodInvocation methodInvocation, ZookeeperEntityManager em, boolean didWeStartWork)
            throws Throwable, Exception {
        Transactional transactional = readTransactionMetadata(methodInvocation);

        final EntityTransaction txn = em.getTransaction();
        txn.begin();

        Object result;
        try {
            result = methodInvocation.proceed();
        } catch (Exception e) {
            // commit transaction only if rollback didnt occur
            if (rollbackIfNecessary(transactional, e, txn)) {
                txn.commit();
            }

            // propagate whatever exception is thrown anyway
            throw e;
        } finally {
            // Close the em if necessary (guarded so this code doesn't
            // run
            // unless catch fired).
            if (didWeStartWork && !txn.isActive()) {
                didWeStartWork = false;
                unitOfWork.end();
            }
        }

        // everything was normal so commit the txn (do not move into try
        // block
        // above as it
        // interferes with the advised method's throwing semantics)
        try {
            txn.commit();
        } finally {
            // close the em if necessary
            if (didWeStartWork) {
                didWeStartWork = false;
                unitOfWork.end();
            }
        }

        // or return result
        return result;
    }

    private int maxAttempts() {
        return 5;
    }

    // TODO(dhanji): Cache this method's results.
    private Transactional readTransactionMetadata(MethodInvocation methodInvocation) {
        Transactional transactional;
        Method method = methodInvocation.getMethod();
        Class<?> targetClass = methodInvocation.getThis().getClass();

        transactional = method.getAnnotation(Transactional.class);
        if (null == transactional) {
            // If none on method, try the class.
            transactional = targetClass.getAnnotation(Transactional.class);
        }
        if (null == transactional) {
            // If there is no transactional annotation present, use the default
            transactional = Internal.class.getAnnotation(Transactional.class);
        }

        return transactional;
    }

    /**
     * Returns True if rollback DID NOT HAPPEN (i.e. if commit should continue).
     * 
     * @param transactional
     *            The metadata annotaiton of the method
     * @param e
     *            The exception to test for rollback
     * @param txn
     *            A JPA Transaction to issue rollbacks on
     */
    private boolean rollbackIfNecessary(Transactional transactional, Exception e, EntityTransaction txn) {
        boolean commit = true;

        if (e instanceof OptimisticLockException) {
            txn.rollback();
            return false;
        }

        // check rollback clauses
        for (Class<? extends Exception> rollBackOn : transactional.rollbackOn()) {

            // if one matched, try to perform a rollback
            if (rollBackOn.isInstance(e)) {
                commit = false;

                // check ignore clauses (supercedes rollback clause)
                for (Class<? extends Exception> exceptOn : transactional.ignore()) {
                    // An exception to the rollback clause was found, DON'T
                    // rollback
                    // (i.e. commit and throw anyway)
                    if (exceptOn.isInstance(e)) {
                        commit = true;
                        break;
                    }
                }

                // rollback only if nothing matched the ignore check
                if (!commit) {
                    txn.rollback();
                }
                // otherwise continue to commit

                break;
            }
        }

        return commit;
    }
}
