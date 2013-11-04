package io.fathom.cloud.identity.commands;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.WellKnownRoles;
import io.fathom.cloud.commands.TypedCmdlet;
import io.fathom.cloud.identity.LoginService;
import io.fathom.cloud.identity.model.AuthenticatedUser;
import io.fathom.cloud.identity.services.IdentityService;
import io.fathom.cloud.identity.services.IdentityService.UserCreationData;
import io.fathom.cloud.protobuf.IdentityModel.DomainData;
import io.fathom.cloud.protobuf.IdentityModel.ProjectData;
import io.fathom.cloud.protobuf.IdentityModel.UserData;

import javax.inject.Inject;

import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserCreateCmdlet extends TypedCmdlet {
    public UserCreateCmdlet() {
        super("id-user-create");
    }

    private static final Logger log = LoggerFactory.getLogger(UserCreateCmdlet.class);

    @Option(name = "-u", usage = "username", required = true)
    public String username;

    @Option(name = "-p", usage = "password", required = true)
    public String password;

    @Inject
    IdentityService identityService;

    @Inject
    LoginService loginService;

    @Override
    protected UserData run0() throws CloudException {
        log.info("Creating user: {}", username);

        DomainData domain = identityService.getDefaultDomain();
        if (domain == null) {
            throw new UnsupportedOperationException();
        }

        UserData.Builder userBuilder = UserData.newBuilder();
        userBuilder.setName(username);
        // userBuilder.setEmail(username);
        userBuilder.setEnabled(true);

        UserData user = identityService.createUser(new UserCreationData(domain, userBuilder, password));

        Long projectId = null;

        AuthenticatedUser authenticatedUser = loginService.authenticate(projectId, username, password);
        if (authenticatedUser == null) {
            throw new IllegalStateException();
        }

        boolean CREATE_PROJECT = false;
        if (CREATE_PROJECT) {
            ProjectData.Builder projectBuilder = ProjectData.newBuilder();
            projectBuilder.setName("default");
            projectBuilder.setDomainId(domain.getId());

            ProjectData project = identityService.createProject(projectBuilder, authenticatedUser,
                    WellKnownRoles.ROLE_ID_ADMIN);
            if (project == null) {
                throw new IllegalStateException();
            }
        }

        return user;
    }

}
