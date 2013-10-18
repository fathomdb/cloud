package io.fathom.cloud.identity.model;

import io.fathom.cloud.identity.secrets.AuthenticatedProjectKeys;
import io.fathom.cloud.identity.secrets.SecretToken;
import io.fathom.cloud.protobuf.IdentityModel.ProjectData;

public class AuthenticatedProject {
    private final ProjectData project;
    private final AuthenticatedProjectKeys keys;

    public AuthenticatedProject(ProjectData project, SecretToken projectSecret) {
        this.project = project;
        this.keys = new AuthenticatedProjectKeys(this, projectSecret);
    }

    public long getProjectId() {
        return project.getId();
    }

    public ProjectData getProject() {
        return project;
    }

    public AuthenticatedProjectKeys getKeys() {
        return keys;
    }
}
