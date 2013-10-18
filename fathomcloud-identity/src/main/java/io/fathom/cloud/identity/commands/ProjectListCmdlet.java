package io.fathom.cloud.identity.commands;

import io.fathom.cloud.commands.TypedCmdlet;
import io.fathom.cloud.identity.services.IdentityService;
import io.fathom.cloud.protobuf.IdentityModel.ProjectData;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProjectListCmdlet extends TypedCmdlet {

    private static final Logger log = LoggerFactory.getLogger(ProjectListCmdlet.class);

    public ProjectListCmdlet() {
        super("id-project-list");
    }

    @Inject
    IdentityService identityService;

    @Override
    protected List<ProjectData> run0() throws Exception {
        List<ProjectData> projects = identityService.listProjects();

        return projects;
    }

}
