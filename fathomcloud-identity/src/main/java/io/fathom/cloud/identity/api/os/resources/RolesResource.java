package io.fathom.cloud.identity.api.os.resources;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.identity.api.os.model.Roles;
import io.fathom.cloud.identity.api.os.model.v2.Role;
import io.fathom.cloud.identity.state.AuthRepository;
import io.fathom.cloud.protobuf.IdentityModel.RoleData;
import io.fathom.cloud.server.auth.Auth;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.inject.persist.Transactional;

@Path("/openstack/identity/v3/roles")
@Transactional
@Produces({ "application/json" })
public class RolesResource extends IdentityResourceBase {
    private static final Logger log = LoggerFactory.getLogger(RolesResource.class);

    // Should use identity service instead
    @Deprecated
    @Inject
    AuthRepository authRepository;

    @GET
    @Produces({ JSON })
    public Roles listRoles() throws CloudException {
        Auth.Domain domain = findDomainWithAdminRole();
        if (domain == null) {
            // TODO: Should we allow a non-admin to list roles?
            // Note that roles are basically public
            throw new WebApplicationException(Status.FORBIDDEN);
        }

        Roles response = new Roles();
        response.roles = Lists.newArrayList();

        for (RoleData model : authRepository.getRoles().list()) {
            response.roles.add(toModel(model));
        }

        return response;
    }

    private Role toModel(RoleData model) {
        Role role = new Role();
        role.id = "" + model.getId();
        role.name = model.getName();
        // if (model.hasDescription()) {
        // role.description = model.getDescription();
        // }
        return role;
    }

}
