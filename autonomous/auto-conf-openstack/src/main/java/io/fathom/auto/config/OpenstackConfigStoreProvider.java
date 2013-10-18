package io.fathom.auto.config;

import io.fathom.auto.config.ConfigStore.ConfigStoreProvider;
import io.fathom.auto.openstack.metadata.Metadata;
import io.fathom.cloud.openstack.client.OpenstackClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenstackConfigStoreProvider extends ConfigStoreProvider {
    private static final Logger log = LoggerFactory.getLogger(OpenstackConfigStoreProvider.class);

    private OpenstackClient client;

    private OpenstackConfig config;

    @Override
    public boolean init() {
        try {
            log.info("Checking for openstack configuration");

            Metadata metadata = getConfig().getMetadata();
            assert metadata != null;

            this.client = getConfig().getOpenstackClient();

            return true;
        } catch (Exception e) {
            log.warn("Unable to build config for OpenStack", e);
            return false;
        }
    }

    private OpenstackConfig getConfig() {
        if (this.config == null) {
            this.config = new OpenstackConfig();
        }
        return this.config;
    }

    @Override
    protected ConfigStore build(String clusterKey, String basePath, String serviceKey) {
        return new OpenstackConfigStore(this, client, clusterKey, basePath, serviceKey);
    }

    @Override
    public String getInstanceProperty(String key) {
        return getConfig().getInstanceProperty(key);
    }
}
