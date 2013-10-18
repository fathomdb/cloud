package io.fathom.cloud.lbaas;

import io.fathom.cloud.OpenstackExtensionBase;
import io.fathom.cloud.ServiceType;
import io.fathom.cloud.lbaas.services.LoadBalanceServiceImpl;
import io.fathom.cloud.loadbalancer.LoadBalanceService;
import io.fathom.cloud.server.model.Project;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathomdb.extensions.HttpConfiguration;
import com.google.common.collect.Lists;

public class LbaasExtension extends OpenstackExtensionBase {
    private static final Logger log = LoggerFactory.getLogger(LbaasExtension.class);

    @Override
    public void addHttpExtensions(HttpConfiguration http) {
    }

    @Override
    protected void configure() {
        bind(LoadBalanceService.class).to(LoadBalanceServiceImpl.class);
    }

    @Override
    public List<ServiceType> getServices(Project project, String baseUrl) {
        List<ServiceType> serviceTypes = Lists.newArrayList();
        if (project != null) {
            // serviceTypes.add(ServiceType.LBAAS);
        }
        return serviceTypes;
    }

}
