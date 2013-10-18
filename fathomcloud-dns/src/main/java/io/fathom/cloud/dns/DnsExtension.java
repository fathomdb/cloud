package io.fathom.cloud.dns;

import io.fathom.cloud.OpenstackExtensionBase;
import io.fathom.cloud.ServiceType;
import io.fathom.cloud.dns.DnsService;
import io.fathom.cloud.dns.api.os.resources.RecordsetsResource;
import io.fathom.cloud.dns.api.os.resources.ZoneResource;
import io.fathom.cloud.dns.services.DnsSecrets;
import io.fathom.cloud.dns.services.DnsServiceImpl;
import io.fathom.cloud.server.model.Project;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathomdb.extensions.HttpConfiguration;
import com.google.common.collect.Lists;

public class DnsExtension extends OpenstackExtensionBase {
    private static final Logger log = LoggerFactory.getLogger(DnsExtension.class);

    @Override
    public void addHttpExtensions(HttpConfiguration http) {
        http.bind(RecordsetsResource.class);
        http.bind(ZoneResource.class);
    }

    @Override
    protected void configure() {
        bind(DnsService.class).to(DnsServiceImpl.class);

        // LifecycleListener
        bind(DnsSecrets.class).asEagerSingleton();
    }

    @Override
    public List<ServiceType> getServices(Project project, String baseUrl) {
        List<ServiceType> serviceTypes = Lists.newArrayList();
        if (project != null) {
            serviceTypes.add(ServiceType.DNS);
        }
        return serviceTypes;
    }

}
