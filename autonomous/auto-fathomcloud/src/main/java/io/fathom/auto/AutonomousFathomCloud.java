package io.fathom.auto;

import io.fathom.auto.config.ConfigPath;
import io.fathom.auto.config.ConfigStore;
import io.fathom.auto.config.ConfigStore.ConfigStoreProvider;
import io.fathom.auto.fathomcloud.CloudServerInstance;
import io.fathom.auto.processes.ProcessExecution;
import io.fathom.auto.processes.Processes;
import io.fathom.auto.zookeeper.ZookeeperInstance;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutonomousFathomCloud {
    private static final Logger log = LoggerFactory.getLogger(AutonomousFathomCloud.class);

    private final ConfigStore configStore;

    AutonomousFathomCloud(ConfigStore configStore) {
        this.configStore = configStore;
    }

    public static void main(String[] args) throws InterruptedException {
        File configFile = new File("/var/fathomcloud/bootstrap");

        ConfigStore configStore = null;

        // if (shouldBootstrap) {
        while (true) {
            try {
                ConfigStoreProvider configStoreProvider = null;
                if (configFile.exists()) {
                    Bootstrap bootstrap = new Bootstrap(configFile);
                    configStoreProvider = bootstrap.bootstrap();
                } else {
                    log.info("Waiting for bootstrap file: {}", configFile);
                }

                if (configStoreProvider != null) {
                    configStore = ConfigStore.get(configStoreProvider, "fathomcloud");
                }
                if (configStore != null) {
                    ensureSshKeys();
                    break;
                }
            } catch (Exception e) {
                log.warn("Error while bootstrapping", e);
            }

            TimeSpan.seconds(2).sleep();
        }

        // } else {
        // configStore = ConfigStore.get("fathomcloud");
        // }

        AutonomousFathomCloud server = new AutonomousFathomCloud(configStore);
        try {
            server.run();
        } catch (Exception e) {
            log.error("Error during fathom cloud run; forcing exit", e);
            System.exit(1);
        }
    }

    private static void ensureSshKeys() throws IOException {
        File sshKey = new File("/home/fathomcloud/.ssh/id_rsa");

        if (!sshKey.exists()) {
            log.info("SSH key not found; calling keygen helper script");

            ProcessBuilder pb = new ProcessBuilder("/opt/manager/keygen.sh");

            // We allow (a very generous) 2 minutes. SSH keygen isn't trivial...
            ProcessExecution execution = Processes.run(pb, TimeSpan.minutes(2));

            if (!execution.didExit()) {
                throw new IOException("Timeout while starting Process");
            } else {
                if (execution.getExitCode() == 0) {
                    log.info("Process started OK");
                }
            }
        }
    }

    public void run() throws IOException, InterruptedException {
        Thread zkThread = new Thread(new Runnable() {
            @Override
            public void run() {
                ConfigPath configRoot = configStore.getConfigRoot();
                ConfigPath zkRoot = configRoot.child("zookeeper");

                ZookeeperInstance zk = new ZookeeperInstance(zkRoot);
                try {
                    zk.run();
                } catch (Exception e) {
                    log.error("Error during ZK run; forcing exit", e);
                    System.exit(1);
                }
            }
        });
        zkThread.start();

        Thread cloudThread = new Thread(new Runnable() {
            @Override
            public void run() {
                CloudServerInstance instance = new CloudServerInstance(configStore);
                try {
                    instance.run();
                } catch (Exception e) {
                    log.error("Error during fathom cloud run; forcing exit", e);
                    System.exit(1);
                }
            }
        });
        cloudThread.start();
    }
}
