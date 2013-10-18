package io.fathom.cloud.identity.secrets;

import io.fathom.cloud.identity.model.AuthenticatedProject;

public class AuthenticatedProjectKeys {
    private final AuthenticatedProject project;
    private final SecretToken projectSecret;

    public AuthenticatedProjectKeys(AuthenticatedProject project, SecretToken projectSecret) {
        this.project = project;
        this.projectSecret = projectSecret;
    }

    SecretToken getSecretToken() {
        return projectSecret;
    }

}
