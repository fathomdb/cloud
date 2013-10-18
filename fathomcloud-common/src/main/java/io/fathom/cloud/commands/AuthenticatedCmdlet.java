package io.fathom.cloud.commands;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.server.auth.Auth;
import io.fathom.cloud.services.AuthService;

import java.util.List;

import javax.inject.Inject;

import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AuthenticatedCmdlet extends TypedCmdlet {
    private static final Logger log = LoggerFactory.getLogger(AuthenticatedCmdlet.class);

    public AuthenticatedCmdlet(String command) {
        super(command);
    }

    @Option(name = "-u", usage = "username", required = true)
    public String username;

    @Option(name = "-p", usage = "password", required = true)
    public String password;

    @Option(name = "-proj", usage = "project", required = false)
    public String projectName;

    @Inject
    protected AuthService authService;

    protected Auth getAuth(String project) throws CloudException {
        Auth unscoped = getUnscopedAuth();
        List<Long> projectIds = authService.resolveProjectName(unscoped, project);

        if (projectIds.size() == 0) {
            throw new IllegalArgumentException("Cannot find project");
        }
        if (projectIds.size() != 1) {
            throw new IllegalArgumentException("The project name is ambiguous");
        }
        Long projectId = projectIds.get(0);

        Auth auth = authService.authenticate(projectId, username, password);
        if (auth == null) {
            throw new IllegalArgumentException("Cannot authenticate to project");
        }
        return auth;
    }

    Auth unscopedAuth;

    protected Auth getUnscopedAuth() throws CloudException {
        if (unscopedAuth == null) {
            Auth unscoped = authService.authenticate(null, username, password);
            if (unscoped == null) {
                throw new IllegalArgumentException("Cannot authenticate");
            }
            this.unscopedAuth = unscoped;
        }

        return unscopedAuth;
    }

    protected Auth getAuth() throws CloudException {
        if (projectName == null) {
            return getUnscopedAuth();
        } else {
            return getAuth(projectName);
        }
    }
}
