package io.fathom.auto;

import io.fathom.auto.config.SecretKeys;
import io.fathom.auto.haproxy.ConfigSync;
import io.fathom.auto.haproxy.HaproxyConfig;

import java.io.File;

public class SimpleHaproxyConfig extends HaproxyConfig {

    final String defaultHost;

    public SimpleHaproxyConfig(SecretKeys secretKeys, String defaultHost) {
        this.secretKeys = secretKeys;
        this.defaultHost = defaultHost;
    }

    @Override
    public ConfigSync getConfigSync(File mirrorPath) {
        return new StaticConfiguration(mirrorPath);
    }

    @Override
    public String getDefaultHost() {
        return defaultHost;
    }

}
