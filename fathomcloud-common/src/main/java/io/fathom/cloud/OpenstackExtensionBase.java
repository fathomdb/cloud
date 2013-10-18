package io.fathom.cloud;

import io.fathom.cloud.server.model.Project;

import java.util.Collections;
import java.util.List;

import com.fathomdb.Configuration;
import com.fathomdb.ConfigurationListener;
import com.fathomdb.extensions.ExtensionModuleBase;

public abstract class OpenstackExtensionBase extends ExtensionModuleBase implements OpenstackExtension,
        ConfigurationListener {

    protected Configuration configuration;

    @Override
    public List<ServiceType> getServices(Project project, String baseUrl) {
        return Collections.emptyList();
    }

    @Override
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }
}
