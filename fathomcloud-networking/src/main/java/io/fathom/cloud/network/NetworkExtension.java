package io.fathom.cloud.network;

import io.fathom.cloud.OpenstackExtensionBase;
import io.fathom.cloud.ServiceType;
import io.fathom.cloud.network.api.os.resources.ExtensionsResource;
import io.fathom.cloud.network.api.os.resources.FloatingIpsResource;
import io.fathom.cloud.network.api.os.resources.NetworkResource;
import io.fathom.cloud.network.api.os.resources.PortsResource;
import io.fathom.cloud.network.api.os.resources.StripExtensionFilter;
import io.fathom.cloud.network.api.os.resources.SubnetsResource;
import io.fathom.cloud.server.model.Project;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathomdb.extensions.HttpConfiguration;
import com.google.common.collect.Lists;

public class NetworkExtension extends OpenstackExtensionBase {
    private static final Logger log = LoggerFactory.getLogger(NetworkExtension.class);

    @Override
    public void addHttpExtensions(HttpConfiguration http) {
        http.bind(ExtensionsResource.class);

        http.bind(FloatingIpsResource.class);
        http.bind(NetworkResource.class);
        http.bind(PortsResource.class);
        http.bind(SubnetsResource.class);

        http.bind(StripExtensionFilter.class);
    }

    @Override
    protected void configure() {
        bind(NetworkService.class).to(NetworkServiceImpl.class);
    }

    @Override
    public List<ServiceType> getServices(Project project, String baseUrl) {
        List<ServiceType> serviceTypes = Lists.newArrayList();
        if (project != null) {
            boolean USE_QUANTUM = false;
            if (USE_QUANTUM) {
                serviceTypes.add(ServiceType.NETWORK);
            }
        }
        return serviceTypes;
    }

}
