package io.fathom.cloud;

import io.fathom.cloud.server.model.Project;

import java.util.List;

import com.fathomdb.extensions.ExtensionModule;

public interface OpenstackExtension extends ExtensionModule {

    List<ServiceType> getServices(Project project, String baseUrl);

}
