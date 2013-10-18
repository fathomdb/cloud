package io.fathom.cloud.identity.commands;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.commands.AuthenticatedCmdlet;
import io.fathom.cloud.identity.AuthServiceImpl;
import io.fathom.cloud.identity.model.AuthenticatedProject;
import io.fathom.cloud.identity.model.AuthenticatedUser;
import io.fathom.cloud.identity.services.IdentityService;
import io.fathom.cloud.protobuf.IdentityModel.DomainData;
import io.fathom.cloud.protobuf.IdentityModel.RoleData;
import io.fathom.cloud.protobuf.IdentityModel.UserData;
import io.fathom.cloud.server.auth.Auth;
import io.fathom.cloud.server.model.Project;

import javax.inject.Inject;

import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.Message;

public class RoleGrantCmdlet extends AuthenticatedCmdlet {
    private static final Logger log = LoggerFactory.getLogger(RoleGrantCmdlet.class);

    public RoleGrantCmdlet() {
        super("id-role-grant");
    }

    @Option(name = "-touser", usage = "user name", required = true)
    public String grantee;

    @Option(name = "-r", usage = "role", required = true)
    public String roleName;

    @Inject
    IdentityService identityService;

    @Override
    public Message run0() throws CloudException {
        if (projectName == null) {
            throw new IllegalArgumentException("Project is required");
        }
        doProjectGrant();
        return null;
    }

    private void doProjectGrant() throws CloudException {
        Auth auth = getAuth();

        Project project = auth.getProject();

        AuthenticatedUser authenticatedUser = ((AuthServiceImpl) authService).toAuthenticatedUser(auth);
        AuthenticatedProject authenticatedProject = identityService.authenticateToProject(authenticatedUser,
                project.getId());

        RoleData role = getRole();

        DomainData domain = identityService.getDefaultDomain();

        UserData grantee = getGrantee(domain);

        log.info("Doing project grant: {} {}", grantee.getName(), role.getName());
        identityService.grantRoleToUserOnProject(authenticatedProject, grantee.getId(), role.getId());
    }

    private UserData getGrantee(DomainData domain) throws CloudException {
        UserData user = identityService.findUserByName(domain.getId(), grantee);
        if (user == null) {
            throw new IllegalArgumentException("Cannot find user: " + grantee);
        }

        return user;
    }

    private RoleData getRole() throws CloudException {
        RoleData role = null;
        for (RoleData r : identityService.listRoles()) {
            if (roleName.equalsIgnoreCase(r.getName())) {
                role = r;
            }
        }
        if (role == null) {
            throw new IllegalArgumentException("Cannot find role: " + roleName);
        }
        return role;
    }
}
