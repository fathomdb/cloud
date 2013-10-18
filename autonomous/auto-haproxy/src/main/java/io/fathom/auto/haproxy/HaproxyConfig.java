package io.fathom.auto.haproxy;

import io.fathom.auto.config.SecretKeys;

import java.io.File;

public abstract class HaproxyConfig {

    protected SecretKeys secretKeys;

    public SecretKeys getSecretKeys() {
        return secretKeys;
    }

    public abstract ConfigSync getConfigSync(File mirrorPath);

    /**
     * The default hostname, which only really matters for SSL
     */
    public abstract String getDefaultHost();
}
