package io.fathom.auto.fathomcloud;

import io.fathom.auto.TimeSpan;
import io.fathom.auto.config.ConfigStore;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudServerInstance {
    private static final Logger log = LoggerFactory.getLogger(CloudServerInstance.class);

    private final ConfigStore configStore;

    CloudServerProcess process;

    public CloudServerInstance(ConfigStore configStore) {
        this.configStore = configStore;
    }

    public void run() throws IOException {
        File instanceDir = new File("/var/fathomcloud");

        if (!instanceDir.exists()) {
            if (!instanceDir.mkdirs()) {
                throw new IOException("Unable to create instance directory");
            }
        }

        // ConfigPath serversPath = configStore.getConfigRoot().child("data");
        FathomCloudConfig config = new FathomCloudConfig(instanceDir);

        while (true) {
            try {
                CloudServerProcess process = CloudServerProcess.find(config);
                if (process == null) {
                    log.info("Process not running; starting");
                    process = CloudServerProcess.start(config);
                }

                // EndpointRegistry registry = new
                // EndpointRegistry(configStore);
                // InetAddress addr = MachineInfo.INSTANCE.getIp();
                // int port = 3306;
                // InetSocketAddress socketAddress = new InetSocketAddress(addr,
                // port);
                // registry.register(ServiceKeys.MYSQL, socketAddress);
                //
                // ConfigPath mysqlConfig = configStore.getConfigRoot();

                while (process.isRunning()) {
                    log.debug("Process is running");
                    TimeSpan.seconds(5).sleep();
                }

                log.warn("Process stopped running / responding");
            } catch (Exception e) {
                log.warn("Error while monitoring", e);
            }
            TimeSpan.seconds(10).sleep();
        }
    }

}
