package io.fathom.auto.fathomcloud;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FathomCloudConfig {
    private static final Logger log = LoggerFactory.getLogger(FathomCloudConfig.class);

    final File instanceDir;

    public FathomCloudConfig(File instanceDir) {
        this.instanceDir = instanceDir;
    }

    public File getInstanceDir() {
        return instanceDir;
    }

    public File getInstallDir() {
        return new File("/opt/fathomcloud");
    }
}
