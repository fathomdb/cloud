package io.fathom.cloud.secrets;

import io.fathom.cloud.OpenstackExtensionBase;
import io.fathom.cloud.ServiceType;
import io.fathom.cloud.secrets.api.os.resources.SecretResource;
import io.fathom.cloud.secrets.services.SecretServiceImpl;
import io.fathom.cloud.server.model.Project;
import io.fathom.cloud.services.SecretService;

import java.util.List;

import com.fathomdb.extensions.HttpConfiguration;
import com.google.common.collect.Lists;

public class SecretsExtension extends OpenstackExtensionBase {

    @Override
    public void addHttpExtensions(HttpConfiguration http) {
        http.bind(SecretResource.class);
    }

    @Override
    protected void configure() {
        bind(SecretService.class).to(SecretServiceImpl.class);
    }

    @Override
    public List<ServiceType> getServices(Project project, String baseUrl) {
        List<ServiceType> serviceTypes = Lists.newArrayList();
        if (project != null) {
            serviceTypes.add(ServiceType.SECRETS);
        }
        return serviceTypes;
    }

}
