package io.fathom.cloud.server.auth;

import io.fathom.cloud.server.model.Project;
import io.fathom.cloud.server.model.User;

public interface Auth {
    boolean checkProject(long projectId);

    User getUser();

    Project getProject();

    Domain findDomainWithAdminRole();

    public interface Domain {
        long getId();
    }
}
