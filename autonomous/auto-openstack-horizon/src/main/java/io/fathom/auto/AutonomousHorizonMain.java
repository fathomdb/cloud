package io.fathom.auto;

import io.fathom.auto.config.ConfigStore;
import io.fathom.auto.config.SecretKeys;
import io.fathom.auto.haproxy.HaproxyInstance;
import io.fathom.auto.openstack.horizon.HorizonInstance;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutonomousHorizonMain {
    private static final Logger log = LoggerFactory.getLogger(AutonomousHorizonMain.class);

    private final ConfigStore configStore;

    AutonomousHorizonMain(ConfigStore configStore) {
        this.configStore = configStore;
    }

    public static void main(String[] args) throws InterruptedException {
        ConfigStore configStore = ConfigStore.get("openstack-horizon");

        AutonomousHorizonMain horizon = new AutonomousHorizonMain(configStore);
        try {
            horizon.run();
        } catch (Exception e) {
            log.error("Error during horizon run; forcing exit", e);
            System.exit(1);
        }
    }

    public void run() throws IOException, InterruptedException {
        final SecretKeys secretKeys = configStore.getSecretKeys();
        final String defaultHost = configStore.getInstanceProperty(ConfigStore.KEY_DNS_HOST);
        log.info("default host = {}", defaultHost);

        Thread haproxyThread = new Thread(new Runnable() {
            @Override
            public void run() {
                SimpleHaproxyConfig config = new SimpleHaproxyConfig(secretKeys, defaultHost);
                HaproxyInstance instance = new HaproxyInstance(config);
                try {
                    instance.run();
                } catch (Exception e) {
                    log.error("Error during haproxy run; forcing exit", e);
                    System.exit(1);
                }
            }
        });
        haproxyThread.start();

        Thread horizonThread = new Thread(new Runnable() {
            @Override
            public void run() {
                HorizonInstance instance = new HorizonInstance(configStore);
                try {
                    instance.run();
                } catch (Exception e) {
                    log.error("Error during horizon run; forcing exit", e);
                    System.exit(1);
                }
            }
        });
        horizonThread.start();
    }
}
