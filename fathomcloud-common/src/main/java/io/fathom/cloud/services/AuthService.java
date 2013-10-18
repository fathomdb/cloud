package io.fathom.cloud.services;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.server.auth.Auth;
import io.fathom.cloud.server.model.Project;

import java.util.List;

public interface AuthService {
    public static final String SYSTEM_PROJECT_NAME = "__system__";

    Auth authenticate(Long projectId, String username, String password) throws CloudException;

    List<Long> resolveProjectName(Auth auth, String projectName) throws CloudException;

    String createServiceToken(Auth auth, long instanceId) throws CloudException;

    String getIdentityUri(String baseUrl);

    Long createProject(Auth auth, String projectName) throws CloudException;

    Project findSystemProject() throws CloudException;
}
