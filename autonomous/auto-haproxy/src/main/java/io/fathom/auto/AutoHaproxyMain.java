package io.fathom.auto;

import io.fathom.auto.config.ConfigStore;
import io.fathom.auto.haproxy.BoundHaproxyConfig;
import io.fathom.auto.haproxy.HaproxyConfig;
import io.fathom.auto.haproxy.HaproxyInstance;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoHaproxyMain {
    private static final Logger log = LoggerFactory.getLogger(AutoHaproxyMain.class);

    private final ConfigStore configStore;

    AutoHaproxyMain(ConfigStore configStore) {
        this.configStore = configStore;
    }

    public static void main(String[] args) throws InterruptedException {
        ConfigStore configStore = ConfigStore.get("lb");

        AutoHaproxyMain main = new AutoHaproxyMain(configStore);
        try {
            main.run();
        } catch (Exception e) {
            log.error("Error during haproxy run; forcing exit", e);
            System.exit(1);
        }
    }

    public void run() throws IOException, InterruptedException {
        Thread haproxyThread = new Thread(new Runnable() {
            @Override
            public void run() {
                HaproxyConfig config = new BoundHaproxyConfig(configStore);
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
    }

}
