package io.fathom.cloud.identity.api.os.resources;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.identity.api.os.model.v2.GetTenantsResponse;
import io.fathom.cloud.identity.api.os.model.v2.TenantDetails;
import io.fathom.cloud.identity.model.AuthenticatedUser;
import io.fathom.cloud.identity.services.IdentityService;
import io.fathom.cloud.protobuf.IdentityModel.ProjectData;
import io.fathom.cloud.protobuf.IdentityModel.ProjectRoles;
import io.fathom.cloud.protobuf.IdentityModel.UserData;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

@Path("/openstack/identity/v2.0/tenants")
@Produces({ "application/json" })
public class TenantsResource extends IdentityResourceBase {
    private static final Logger log = LoggerFactory.getLogger(TenantsResource.class);

    @Inject
    IdentityService identityService;

    @GET
    public GetTenantsResponse doTenantsGet() throws CloudException {
        GetTenantsResponse response = new GetTenantsResponse();
        List<TenantDetails> tenants = response.tenants = Lists.newArrayList();

        UserData user = getUser();
        AuthenticatedUser authenticated = getAuthenticatedUser();

        for (ProjectRoles projectRole : user.getProjectRolesList()) {
            long projectId = projectRole.getProject();

            ProjectData project = identityService.findProject(authenticated, projectId);
            if (project == null) {
                log.warn("Cannot find project {}", projectId);
                continue;
            }

            TenantDetails tenant = toTenantDetails(project);
            tenants.add(tenant);
        }

        return response;
    }

    private TenantDetails toTenantDetails(ProjectData project) {
        TenantDetails tenant = new TenantDetails();

        tenant.id = "" + project.getId();
        tenant.name = project.getName();
        tenant.description = project.getDescription();

        if (project.hasEnabled()) {
            tenant.enabled = project.getEnabled();
        } else {
            tenant.enabled = true;
        }

        return tenant;
    }

}
