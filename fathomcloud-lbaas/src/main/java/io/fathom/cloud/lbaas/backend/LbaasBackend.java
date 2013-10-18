package io.fathom.cloud.lbaas.backend;

import io.fathom.cloud.server.model.Project;

public interface LbaasBackend {
    void updateHost(Project project, String host);
}
