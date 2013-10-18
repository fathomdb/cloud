package io.fathom.auto.haproxy;

import io.fathom.auto.TimeSpan;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HaproxyInstance {
    private static final Logger log = LoggerFactory.getLogger(HaproxyInstance.class);

    private HaproxyProcess haproxyProcess;

    private final HaproxyConfig config;

    public HaproxyInstance(HaproxyConfig config) {
        this.config = config;
    }

    public void run() throws IOException {
        File mirrorDir = new File("/var/haproxy/mirror/data");
        File keysDir = new File("/var/haproxy/keys");

        mkdirs(mirrorDir);
        mkdirs(keysDir);

        ConfigSync configSync = config.getConfigSync(mirrorDir);

        boolean firstSyncDone = false;
        while (!firstSyncDone) {
            boolean readOkay = false;
            try {

                int errors = configSync.firstSync();
                if (errors == 0) {
                    log.info("Completed initial sync successfully");
                    readOkay = true;
                } else {
                    log.info("Errors while syncing; count={}", errors);
                }
            } catch (Exception e) {
                log.error("Error during initial sync", e);
            }

            if (readOkay) {
                try {
                    installNewConfigFile(mirrorDir, keysDir);
                    configSync.markClean();
                    firstSyncDone = true;
                } catch (Exception e) {
                    log.error("Error installing haproxy config", e);
                }
            }

            TimeSpan.seconds(1).sleep();
        }

        while (true) {
            try {
                // TODO: Subscribe for notifications??
                log.info("Checking for DB updates");

                int errors = configSync.refresh();
                if (errors == 0) {
                    log.info("Completed refresh successfully");

                    if (configSync.isDirty()) {
                        try {
                            installNewConfigFile(mirrorDir, keysDir);
                            configSync.markClean();
                        } catch (Exception e) {
                            log.error("Error installing haproxy config", e);
                        }
                    }
                    TimeSpan.minutes(1).sleep();
                } else {
                    log.info("Errors during refresh: {}", errors);
                    TimeSpan.seconds(1).sleep();
                }
            } catch (Exception e) {
                log.error("Error during refresh", e);
            }
        }

    }

    private static void mkdirs(File dir) throws IOException {
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new IOException("Unable to create directory: " + dir.getAbsolutePath());
            }
        }
    }

    private void installNewConfigFile(File mirrorDir, File keysDir) throws IOException, InterruptedException {
        File tempFile = null;
        try {
            HaproxyConfigBuilder target = new HaproxyConfigBuilder(mirrorDir, keysDir, config.getSecretKeys());

            target.setDefaultHost(config.getDefaultHost());

            tempFile = File.createTempFile("haproxy", "cfg");

            // TODO: Don't refresh every time??
            log.warn("We refresh the secret keys every time");
            config.getSecretKeys().refresh();

            target.visitDir(target.getMirrorDir());

            try (FileWriter writer = new FileWriter(tempFile)) {
                target.generateConfig(writer);
            }

            if (!HaproxyProcess.validate(tempFile)) {
                throw new IllegalStateException("Haproxy returned error when validating file");
            }

            // TODO: Backup old config file??

            log.info("Installing new config file");
            tempFile.renameTo(HaproxyProcess.CONFIG_FILE);
            tempFile = null;

            ensureHaproxy();
        } finally {
            if (tempFile != null) {
                tempFile.delete();
            }
        }
    }

    private void ensureHaproxy() throws IOException, InterruptedException {
        if (haproxyProcess == null) {
            haproxyProcess = HaproxyProcess.find();
        }

        if (haproxyProcess == null) {
            haproxyProcess = HaproxyProcess.start();
        } else {
            haproxyProcess.reload();
        }
    }
}
