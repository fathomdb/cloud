package io.fathom.cloud.persist;

import io.fathom.cloud.zookeeper.ZookeeperClient;

import javax.inject.Inject;

public class ZookeeperEntityManagerFactory {

    @Inject
    ZookeeperClient zkClient;

    boolean open = true;

    public ZookeeperEntityManager createEntityManager() {
        return new ZookeeperEntityManager();
    }

    public boolean isOpen() {
        return open;
    }

    public void close() {
        this.open = false;
    }

}
