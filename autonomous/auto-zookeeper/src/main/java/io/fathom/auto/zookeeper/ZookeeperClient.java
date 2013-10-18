package io.fathom.auto.zookeeper;

import io.fathom.auto.InterruptedError;

import java.io.IOException;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooKeeper.States;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;

public class ZookeeperClient {
    private static final Logger log = LoggerFactory.getLogger(ZookeeperClient.class);

    private final String connectString;

    public ZookeeperClient(String connectString) {
        this.connectString = connectString;
    }

    ZooKeeper zk;

    public synchronized ZooKeeper getZk() throws IOException {
        if (zk == null) {
            int sessionTimeout = 5000;
            Watcher watcher = new Watcher() {

                @Override
                public void process(WatchedEvent event) {
                    onWatchEvent(event);
                }

            };
            zk = new ZooKeeper(connectString, sessionTimeout, watcher);
        }
        return zk;
    }

    protected void onWatchEvent(WatchedEvent event) {
        log.info("Got ZK event: {}", event);
    }

    /**
     * Note this is only for versions > 3.4.5
     */
    public String getConfig() throws IOException, KeeperException {
        Stat stat = new Stat();
        byte[] config;
        try {
            config = getZk().getConfig(false, stat);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InterruptedError(e);
        }
        String s = new String(config, Charsets.UTF_8);

        return s;
    }

    public States getState() throws IOException, KeeperException {
        States states = getZk().getState();
        return states;
    }
}
