package io.fathom.cloud.services;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.server.auth.Auth;
import io.fathom.cloud.server.model.Project;

public interface Attachments {
    public interface ClientApp {
        String getAppName();

        String getAppId();
    }

    ClientApp findClientAppByName(Project project, String appName, String secret) throws CloudException;

    ClientApp findClientAppById(String appId, String secret) throws CloudException;

    ClientApp createClientApp(Auth auth, Project project, String name, String appPassword) throws CloudException;

    byte[] findUserSecret(ClientApp app, Auth auth) throws CloudException;

    void setUserSecret(ClientApp app, Auth auth, byte[] payload) throws CloudException;

    byte[] findProjectSecret(ClientApp app, Auth auth, Project project) throws CloudException;

    void setProjectSecret(ClientApp app, Auth auth, Project project, byte[] payload) throws CloudException;

}
