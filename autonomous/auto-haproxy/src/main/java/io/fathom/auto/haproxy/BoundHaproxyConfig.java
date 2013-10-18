package io.fathom.auto.haproxy;

import io.fathom.auto.config.ConfigPath;
import io.fathom.auto.config.ConfigStore;

import java.io.File;

public class BoundHaproxyConfig extends HaproxyConfig {
    private final ConfigStore configStore;

    public BoundHaproxyConfig(ConfigStore configStore) {
        this.configStore = configStore;

        this.secretKeys = configStore.getSecretKeys();
    }

    @Override
    public ConfigSync getConfigSync(File mirrorPath) {
        ConfigPath serversPath = configStore.getConfigRoot().child("data");
        return new ServerConfig(serversPath, mirrorPath);
    }

    @Override
    public String getDefaultHost() {
        return null;
    }

}
