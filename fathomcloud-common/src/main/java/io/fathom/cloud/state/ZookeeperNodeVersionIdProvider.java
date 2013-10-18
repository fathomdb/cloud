package io.fathom.cloud.state;

import io.fathom.cloud.zookeeper.ZookeeperClient;

import java.io.IOException;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZookeeperNodeVersionIdProvider implements IdProvider {
    // See:
    // http://zookeeper-user.578899.n2.nabble.com/Sequence-Number-Generation-With-Zookeeper-td5378618.html

    private static final Logger log = LoggerFactory.getLogger(ZookeeperNodeVersionIdProvider.class);

    final ZookeeperClient client;
    final String path;

    public ZookeeperNodeVersionIdProvider(ZookeeperClient client, String path) {
        super();
        this.client = client;
        this.path = path;
    }

    static final byte[] VALUE = new byte[0];

    @Override
    public int get() {
        boolean autoCreate = true;

        while (true) {
            try {
                try {
                    Stat stat = client.setData(path, VALUE, -1);
                    return stat.getVersion();
                } catch (NoNodeException e) {
                    if (autoCreate) {
                        log.info("Node not found; creating: " + path);

                        autoCreate = false;

                        try {
                            client.create(path, VALUE, true);
                        } catch (NodeExistsException e1) {
                            log.info("Node concurrently created: " + path, e1);
                        }
                    } else {
                        throw e;
                    }
                }
            } catch (KeeperException e) {
                throw new IllegalStateException("Error communicating with zookeeper", e);
            } catch (IOException e) {
                throw new IllegalStateException("Error communicating with zookeeper", e);
            }
        }

    }
}
