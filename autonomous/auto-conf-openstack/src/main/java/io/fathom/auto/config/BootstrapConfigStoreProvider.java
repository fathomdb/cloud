package io.fathom.auto.config;

import io.fathom.auto.config.ConfigStore.ConfigStoreProvider;
import io.fathom.cloud.openstack.client.OpenstackClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BootstrapConfigStoreProvider extends ConfigStoreProvider {
    private static final Logger log = LoggerFactory.getLogger(BootstrapConfigStoreProvider.class);

    final OpenstackClient openstackClient;

    public BootstrapConfigStoreProvider(OpenstackClient openstackClient) {
        this.openstackClient = openstackClient;
    }

    @Override
    public boolean init() {
        log.info("Using bootstrap configuration");
        return true;
    }

    @Override
    protected ConfigStore build(String clusterKey, String basePath, String serviceKey) {
        return new OpenstackConfigStore(this, openstackClient, clusterKey, basePath, serviceKey);
    }

    @Override
    public String getInstanceProperty(String key) {
        log.warn("No instance properties for bootstrap");
        return null;
    }
}
