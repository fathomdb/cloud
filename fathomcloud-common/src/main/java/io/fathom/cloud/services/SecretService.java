package io.fathom.cloud.services;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.server.auth.Auth;
import io.fathom.cloud.server.model.Project;

import java.util.List;

public interface SecretService {
    public interface SecretItem {
        byte[] getBytes();
    }

    public interface Secret {
        SecretItem find(String key);

        SecretInfo getSecretInfo();

        long getId();
    }

    public static class SecretInfo {
        public String name;
        public String algorithm;
        public int keySize;
        public String subject;
    }

    List<Secret> list(Auth auth, Project project) throws CloudException;

    Secret find(Auth auth, Project project, long id) throws CloudException;

    Secret setSecretItem(Auth auth, Secret secret, String key, byte[] data) throws CloudException;

    Secret create(Auth auth, Project project, SecretInfo secretInfo) throws CloudException;

    Secret deleteKey(Auth auth, Project project, long id) throws CloudException;

}
