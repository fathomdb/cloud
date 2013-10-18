package io.fathom.auto.zookeeper;

import io.fathom.auto.TimeSpan;

import java.io.IOException;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper.States;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SupervisedZookeeper {
    private static final Logger log = LoggerFactory.getLogger(SupervisedZookeeper.class);

    private ZookeeperProcess process;

    private ZookeeperClient client;

    private final ZookeeperConfig config;

    private SupervisedZookeeper(ZookeeperConfig config, ZookeeperProcess process) {
        this.config = config;
        this.process = process;
    }

    public synchronized void start() throws IOException {
        if (this.process == null) {
            this.process = ZookeeperProcess.start(config);
        }

        this.client = waitForZkAlive();
    }

    public void monitor() {
        monitorCluster(getClient());
    }

    private ZookeeperClient getClient() {
        if (this.client == null) {
            this.client = waitForZkAlive();
        }
        return this.client;
    }

    private static ZookeeperClient waitForZkAlive() {
        ZookeeperClient client = new ZookeeperClient("127.0.0.1:2181");

        while (true) {
            try {
                log.info("Waiting for zookeeper server to start");

                States state = client.getState();
                log.info("Zookeeper state: {}", state);
                if (state.isConnected()) {
                    // String config = client.getConfig();
                    // log.info("Zookeeper config: {}", config);

                    return client;
                }
            } catch (IOException e) {
                log.error("Error connecting to ZK", e);
            } catch (KeeperException e) {
                log.error("Error from ZK", e);
            }

            TimeSpan.seconds(1).sleep();
        }
    }

    private static void monitorCluster(ZookeeperClient zk) {
        while (true) {
            try {
                log.info("Checking zookeeper status");

                States state = zk.getState();
                if (state.isConnected()) {
                    log.info("Connection status: {}", state);
                    // String config = zk.getConfig();
                    // log.info("Cluster state: {}", config);
                } else {
                    log.error("Not connected to ZK: {}", state);
                }
            } catch (IOException e) {
                log.error("Error connecting to ZK", e);
            } catch (KeeperException e) {
                log.error("Error from ZK", e);
            }

            // We're local; we can poll quickly...
            TimeSpan.seconds(10).sleep();
        }

    }

    public static SupervisedZookeeper build(ZookeeperConfig config) throws IOException {
        ZookeeperProcess process = ZookeeperProcess.find(config);
        return new SupervisedZookeeper(config, process);
    }

    public synchronized boolean isRunning() {
        return process != null;
    }
}
