package io.fathom.cloud.lbaas.backend;

import io.fathom.cloud.lbaas.services.LoadBalanceServiceImpl;
import io.fathom.cloud.lbaas.state.LbaasRepository;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class LbaasBackendBase implements LbaasBackend {
    private static final Logger log = LoggerFactory.getLogger(LbaasBackendBase.class);

    @Inject
    protected LbaasRepository repository;

    @Inject
    protected LoadBalanceServiceImpl lbaas;

}
