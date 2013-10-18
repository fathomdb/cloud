package io.fathom.cloud.dbaas.api.os.resources;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.server.model.Project;
import io.fathom.cloud.server.resources.OpenstackResourceBase;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbaasResourceBase extends OpenstackResourceBase {
    private static final Logger log = LoggerFactory.getLogger(DbaasResourceBase.class);

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

    protected String getProjectKey() throws CloudException {
        String uri = httpRequest.getRequestURI();
        if (!uri.startsWith("/")) {
            uri = "/" + uri;
        }
        if (uri.startsWith("/openstack/dbaas/")) {
            uri = uri.substring("/openstack/dbaas/".length());
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
