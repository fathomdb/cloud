package io.fathom.auto.zookeeper;

import io.fathom.auto.config.ConfigStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutonomousZookeeperMain {
    private static final Logger log = LoggerFactory.getLogger(AutonomousZookeeperMain.class);

    public static void main(String[] args) throws InterruptedException {
        ConfigStore configStore = ConfigStore.get("zookeeper");

        ZookeeperInstance zk = new ZookeeperInstance(configStore.getConfigRoot());
        try {
            zk.run();
        } catch (Exception e) {
            log.error("Error during ZK run; forcing exit", e);
            System.exit(1);
        }
    }

}
