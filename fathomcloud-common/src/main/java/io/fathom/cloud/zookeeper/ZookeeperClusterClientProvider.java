package io.fathom.cloud.zookeeper;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

@Singleton
public class ZookeeperClusterClientProvider implements Provider<ZookeeperClient> {

    @Inject
    ZookeeperCluster zkCluster;

    @Override
    public ZookeeperClient get() {
        ZookeeperClient zk = zkCluster.getZookeeperClient();

        return zk;
    }

}
