package io.fathom.cloud.identity.commands;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.commands.AuthenticatedCmdlet;
import io.fathom.cloud.identity.AuthServiceImpl;
import io.fathom.cloud.identity.model.AuthenticatedUser;
import io.fathom.cloud.identity.services.IdentityService;
import io.fathom.cloud.server.auth.Auth;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.Message;

public class ProjectFixupCmdlet extends AuthenticatedCmdlet {

    private static final Logger log = LoggerFactory.getLogger(ProjectFixupCmdlet.class);

    public ProjectFixupCmdlet() {
        super("id-project-fixup");
    }

    @Inject
    IdentityService identityService;

    @Override
    protected Message run0() throws Exception {
        Auth auth = getUnscopedAuth();
        if (auth == null) {
            throw new IllegalStateException("Cannot authenticate as " + username);
        }
        log.info("Authenticated as: {}", username);

        Long projectId = findProjectId(auth, projectName);

        AuthenticatedUser authenticatedUser = ((AuthServiceImpl) authService).toAuthenticatedUser(auth);

        identityService.fixupProject(authenticatedUser, projectId);

        return null;
    }

    private Long findProjectId(Auth auth, String projectName) throws CloudException {
        List<Long> projectIds = authService.resolveProjectName(auth, projectName);

        if (projectIds.size() == 0) {
            throw new IllegalArgumentException("Cannot find project");
        }
        if (projectIds.size() != 1) {
            throw new IllegalArgumentException("The project name is ambiguous");
        }
        Long projectId = projectIds.get(0);

        return projectId;
    }

}
