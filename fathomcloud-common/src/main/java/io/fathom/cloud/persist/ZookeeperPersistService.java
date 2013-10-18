package io.fathom.cloud.persist;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.PersistService;
import com.google.inject.persist.UnitOfWork;

@Singleton
class ZookeeperPersistService implements Provider<ZookeeperEntityManager>, UnitOfWork, PersistService {

    private final ThreadLocal<ZookeeperEntityManager> entityManager = new ThreadLocal<ZookeeperEntityManager>();

    // private final String persistenceUnitName;
    // private final Properties persistenceProperties;
    //
    // @Inject
    // public ZookeeperPersistService(@Jpa String persistenceUnitName,
    // @Nullable @Jpa Properties persistenceProperties) {
    // this.persistenceUnitName = persistenceUnitName;
    // this.persistenceProperties = persistenceProperties;
    // }

    @Override
    public ZookeeperEntityManager get() {
        if (!isWorking()) {
            begin();
        }

        ZookeeperEntityManager em = entityManager.get();
        Preconditions.checkState(null != em, "Requested EntityManager outside work unit. "
                + "Try calling UnitOfWork.begin() first, or use a PersistFilter if you "
                + "are inside a servlet environment.");

        return em;
    }

    public boolean isWorking() {
        return entityManager.get() != null;
    }

    @Override
    public void begin() {
        Preconditions.checkState(null == entityManager.get(),
                "Work already begun on this thread. Looks like you have called UnitOfWork.begin() twice"
                        + " without a balancing call to end() in between.");

        entityManager.set(emFactory.createEntityManager());
    }

    @Override
    public void end() {
        ZookeeperEntityManager em = entityManager.get();

        // Let's not penalize users for calling end() multiple times.
        if (null == em) {
            return;
        }

        em.close();
        entityManager.remove();
    }

    private volatile ZookeeperEntityManagerFactory emFactory = new ZookeeperEntityManagerFactory();

    @Override
    public synchronized void start() {
        // if (this.emFactory == null) {
        // Preconditions.checkState(null == emFactory,
        // "Persistence service was already initialized.");
        //
        // // if (null != persistenceProperties) {
        // // this.emFactory = Persistence.createEntityManagerFactory(
        // // persistenceUnitName, persistenceProperties);
        // // } else {
        // // this.emFactory = Persistence
        // // .createEntityManagerFactory(persistenceUnitName);
        // // }
        //
        // this.emFactory = new ZookeeperEntityManagerFactory();
        // }
    }

    @Override
    public synchronized void stop() {
        // Preconditions.checkState(emFactory.isOpen(),
        // "Persistence service was already shut down.");
        // emFactory.close();
    }

    @Singleton
    public static class EntityManagerFactoryProvider implements Provider<ZookeeperEntityManagerFactory> {
        private final ZookeeperPersistService emProvider;

        @Inject
        public EntityManagerFactoryProvider(ZookeeperPersistService emProvider) {
            this.emProvider = emProvider;
        }

        @Override
        public ZookeeperEntityManagerFactory get() {
            assert null != emProvider.emFactory;
            return emProvider.emFactory;
        }
    }
}
