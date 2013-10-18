package io.fathom.cloud.dbaas;

import io.fathom.cloud.dbaas.api.os.resources.DbBackupsResource;
import io.fathom.cloud.dbaas.api.os.resources.DbInstancesResource;
import io.fathom.cloud.services.DbaasService;

import com.fathomdb.extensions.ExtensionModuleBase;
import com.fathomdb.extensions.HttpConfiguration;

public class DbaasExtension extends ExtensionModuleBase {

    @Override
    public void addHttpExtensions(HttpConfiguration http) {
        http.bind(DbBackupsResource.class);
        http.bind(DbInstancesResource.class);
    }

    @Override
    protected void configure() {
        bind(DbaasService.class).to(DbaasServiceImpl.class);
    }

}
