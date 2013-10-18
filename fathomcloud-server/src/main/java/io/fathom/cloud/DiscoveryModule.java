package io.fathom.cloud;

import com.fathomdb.discovery.Discovery;
import com.google.inject.AbstractModule;

public class DiscoveryModule extends AbstractModule {

    final Discovery discovery;

    public DiscoveryModule(Discovery discovery) {
        this.discovery = discovery;
    }

    @Override
    protected void configure() {
        bind(Discovery.class).toInstance(discovery);
    }

}
