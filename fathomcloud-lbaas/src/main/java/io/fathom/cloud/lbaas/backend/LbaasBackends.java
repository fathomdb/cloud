package io.fathom.cloud.lbaas.backend;

import io.fathom.cloud.lbaas.backend.selfhosted.SelfHostedLbaasBackend;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.persist.Transactional;

@Singleton
@Transactional
public class LbaasBackends {
    private static final Logger log = LoggerFactory.getLogger(LbaasBackends.class);

    @Inject
    Provider<SelfHostedLbaasBackend> selfHostedProvider;

    public LbaasBackend getBackend() {
        SelfHostedLbaasBackend selfHosted = selfHostedProvider.get();
        return selfHosted;
    }
}
