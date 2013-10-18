package io.fathom.cloud.persist;

public class ZookeeperEntityManager {

    public void close() {

    }

    ZookeeperEntityTransaction txn = new ZookeeperEntityTransaction();

    public ZookeeperEntityTransaction getTransaction() {
        return txn;
    }

}
