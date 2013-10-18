package io.fathom.cloud.compute.api.os.resources;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.compute.state.ComputeRepository;
import io.fathom.cloud.server.auth.Auth;
import io.fathom.cloud.server.auth.Auth.Domain;
import io.fathom.cloud.server.model.Project;
import io.fathom.cloud.server.resources.OpenstackResourceBase;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComputeResourceBase extends OpenstackResourceBase {
    private static final Logger log = LoggerFactory.getLogger(ComputeResourceBase.class);

    @Inject
    ComputeRepository computeStore;

    private Project project;

    protected Project getProject() throws CloudException {
        if (project == null) {
            project = findProject(getProjectKey());
        }
        if (project == null) {
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }
        return project;
    }

    @Deprecated
    // This is a dark corner of the OpenStack API
    protected void checkDomainAdmin() {
        Auth auth = getAuth();

        Domain domainAdmin = auth.findDomainWithAdminRole();
        if (domainAdmin == null) {
            log.debug("Expected domain admin: {}", auth);
            throw new WebApplicationException(Status.FORBIDDEN);
        }
    }

    protected String getProjectKey() throws CloudException {
        String uri = httpRequest.getRequestURI();
        if (!uri.startsWith("/")) {
            uri = "/" + uri;
        }
        if (uri.startsWith("/openstack/compute/")) {
            uri = uri.substring("/openstack/compute/".length());
        }

        int slashIndex = uri.indexOf('/');
        if (slashIndex != -1) {
            uri = uri.substring(0, slashIndex);
        }

        if (uri.contains("/")) {
            throw new IllegalStateException();
        }

        return uri;
    }

}
