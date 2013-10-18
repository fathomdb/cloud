package io.fathom.cloud.server.model;

public class Project {

    private final long projectId;

    public Project(long projectId) {
        this.projectId = projectId;
    }

    public long getId() {
        return projectId;
    }

}
