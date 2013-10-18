package io.fathom.cloud.identity.commands;

import io.fathom.cloud.commands.Cmdlet;
import io.fathom.cloud.server.auth.Auth;
import io.fathom.cloud.server.model.Project;
import io.fathom.cloud.services.AuthService;

import java.util.List;

import javax.inject.Inject;

import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProjectCreateCmdlet extends Cmdlet {

    private static final Logger log = LoggerFactory.getLogger(ProjectCreateCmdlet.class);

    @Option(name = "-u", usage = "username", required = true)
    public String username;

    @Option(name = "-p", usage = "password", required = true)
    public String password;

    @Option(name = "-proj", usage = "project", required = true)
    public String projectName;

    public ProjectCreateCmdlet() {
        super("id-project-create");
    }

    @Inject
    AuthService authService;

    @Override
    protected void run() throws Exception {
        Auth auth = authService.authenticate(null, username, password);
        if (auth == null) {
            throw new IllegalStateException("Cannot authenticate as " + username);
        }
        log.info("Authenticated as: {}", username);

        List<Long> projectIds = authService.resolveProjectName(auth, projectName);
        Long projectId;
        if (projectIds.isEmpty()) {
            log.info("Creating project: {}", projectName);
            projectId = authService.createProject(auth, projectName);
        } else {
            throw new IllegalArgumentException("Project already exists");
        }

        auth = authService.authenticate(projectId, username, password);
        if (auth == null) {
            throw new IllegalStateException("Error authenticating to project");
        }
        log.info("Authenticated to project");

        Project project = auth.getProject();
        println("Created project: %s", project.getId());
    }

}
