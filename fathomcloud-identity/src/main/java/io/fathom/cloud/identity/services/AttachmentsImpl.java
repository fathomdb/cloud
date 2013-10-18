package io.fathom.cloud.identity.services;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.identity.AuthServiceImpl;
import io.fathom.cloud.identity.model.AuthenticatedProject;
import io.fathom.cloud.identity.model.AuthenticatedUser;
import io.fathom.cloud.identity.secrets.AppSecrets;
import io.fathom.cloud.identity.secrets.SecretToken;
import io.fathom.cloud.identity.secrets.Secrets;
import io.fathom.cloud.identity.secrets.SecretToken.SecretTokenType;
import io.fathom.cloud.identity.state.AuthRepository;
import io.fathom.cloud.protobuf.CloudCommons.SecretData;
import io.fathom.cloud.protobuf.IdentityModel.AttachmentData;
import io.fathom.cloud.protobuf.IdentityModel.ClientAppData;
import io.fathom.cloud.protobuf.IdentityModel.ClientAppSecretData;
import io.fathom.cloud.protobuf.IdentityModel.SecretStoreData;
import io.fathom.cloud.server.auth.Auth;
import io.fathom.cloud.server.model.Project;
import io.fathom.cloud.services.Attachments;
import io.fathom.cloud.state.DuplicateValueException;
import io.fathom.cloud.state.NamedItemCollection;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.keyczar.exceptions.KeyczarException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.inject.persist.Transactional;

@Singleton
@Transactional
public class AttachmentsImpl implements Attachments {
    private static final Logger log = LoggerFactory.getLogger(AttachmentsImpl.class);

    @Inject
    AuthRepository authRepository;

    @Inject
    AppSecrets appSecrets;

    @Inject
    IdentityService identityService;

    @Inject
    AuthServiceImpl authService;

    protected NamedItemCollection<ClientAppData> getStore(long projectId) {
        NamedItemCollection<ClientAppData> store = authRepository.getClientApps(projectId);
        return store;
    }

    public static class ClientAppImpl implements ClientApp {
        final ClientAppData data;
        final ClientAppSecretData secrets;

        public ClientAppImpl(ClientAppData data, ClientAppSecretData secrets) {
            this.data = data;
            this.secrets = secrets;
        }

        @Override
        public String getAppName() {
            return data.getKey();
        }

        @Override
        public String getAppId() {
            return data.getProject() + ":" + data.getKey();
        }

    }

    @Override
    public ClientApp findClientAppByName(Project project, String appName, String appPassword) throws CloudException {
        ClientAppData clientApp = getStore(project.getId()).find(appName);
        if (clientApp == null) {
            log.debug("App '{}' not found in project", appName);
            return null;
        }

        ClientAppSecretData secret = findClientAppSecretData(clientApp, appPassword);
        if (secret == null) {
            log.debug("Password mismatch on app '{}'", appName);
            return null;
        }

        return new ClientAppImpl(clientApp, secret);
    }

    @Override
    public ClientApp findClientAppById(String appId, String appPassword) throws CloudException {
        int firstColon = appId.indexOf(':');
        if (firstColon == -1) {
            log.debug("Invalid app id: " + appId);
            return null;
        }

        String project = appId.substring(0, firstColon);
        String appName = appId.substring(firstColon + 1);
        ClientAppData clientApp = getStore(Long.valueOf(project)).find(appName);
        if (clientApp == null) {
            log.debug("App '{}' not found in project", appId);
            return null;
        }

        ClientAppSecretData secret = findClientAppSecretData(clientApp, appPassword);
        if (secret == null) {
            log.debug("Password mismatch on app '{}'", appId);
            return null;
        }

        return new ClientAppImpl(clientApp, secret);
    }

    @Override
    public void setUserSecret(ClientApp app, Auth auth, byte[] payload) throws CloudException {
        AuthenticatedUser user = authService.toAuthenticatedUser(auth);

        long userId = user.getUserId();
        NamedItemCollection<AttachmentData> store = authRepository.getUserAttachments(userId);

        SecretData.Builder s = SecretData.newBuilder();
        try {
            appSecrets.setUserSecret(user, s, payload);
        } catch (KeyczarException e) {
            throw new IllegalStateException("Error setting secret", e);
        }

        setSecret(store, app, s.build());

    }

    @Override
    public void setProjectSecret(ClientApp app, Auth auth, Project project, byte[] payload) throws CloudException {
        AuthenticatedUser user = authService.toAuthenticatedUser(auth);

        Project authProject = auth.getProject();
        if (project == null) {
            throw new IllegalArgumentException();
        }

        if (authProject.getId() != project.getId()) {
            throw new IllegalArgumentException();
        }

        long projectId = project.getId();

        AuthenticatedProject authenticatedProject = identityService.authenticateToProject(user, projectId);
        if (authenticatedProject == null) {
            throw new IllegalStateException();
        }

        NamedItemCollection<AttachmentData> store = authRepository.getProjectAttachments(projectId);

        SecretData.Builder s = SecretData.newBuilder();
        try {
            appSecrets.setProjectSecret(authenticatedProject, s, payload);
        } catch (KeyczarException e) {
            throw new IllegalStateException("Error setting secret", e);
        }

        setSecret(store, app, s.build());

    }

    private void setSecret(NamedItemCollection<AttachmentData> store, ClientApp app, SecretData data)
            throws CloudException {
        String appKey = app.getAppId();
        AttachmentData existing = store.find(appKey);
        AttachmentData.Builder b;
        if (existing == null) {
            b = AttachmentData.newBuilder();
            b.setKey(appKey);
        } else {
            b = AttachmentData.newBuilder(existing);
        }

        b.setData(data);

        if (existing == null) {
            try {
                store.create(b);
            } catch (DuplicateValueException e) {
                throw new IllegalStateException();
            }
        } else {
            store.update(b);
        }
    }

    @Override
    public byte[] findUserSecret(ClientApp app, Auth auth) throws CloudException {
        AuthenticatedUser user = authService.toAuthenticatedUser(auth);

        long userId = user.getUserId();
        NamedItemCollection<AttachmentData> store = authRepository.getUserAttachments(userId);

        return findUserSecret(store, app, user);
    }

    @Override
    public byte[] findProjectSecret(ClientApp app, Auth auth, Project project) throws CloudException {
        AuthenticatedUser user = authService.toAuthenticatedUser(auth);

        Project authProject = auth.getProject();
        if (project == null) {
            throw new IllegalArgumentException();
        }

        if (authProject.getId() != project.getId()) {
            throw new IllegalArgumentException();
        }

        long projectId = project.getId();

        AuthenticatedProject authenticatedProject = identityService.authenticateToProject(user, projectId);
        if (authenticatedProject == null) {
            throw new IllegalStateException();
        }

        NamedItemCollection<AttachmentData> store = authRepository.getProjectAttachments(projectId);

        return findProjectSecret(store, app, authenticatedProject);
    }

    private byte[] findUserSecret(NamedItemCollection<AttachmentData> store, ClientApp app, AuthenticatedUser user)
            throws CloudException {
        String appKey = app.getAppId();
        AttachmentData existing = store.find(appKey);
        if (existing == null) {
            return null;
        }

        SecretData secretData = existing.getData();

        try {
            byte[] plaintext = appSecrets.decryptUserSecret(user, secretData);
            return plaintext;
        } catch (KeyczarException e) {
            throw new IllegalStateException("Error decrypting secret", e);
        }
    }

    private byte[] findProjectSecret(NamedItemCollection<AttachmentData> store, ClientApp app,
            AuthenticatedProject project) throws CloudException {
        String appKey = app.getAppId();
        AttachmentData existing = store.find(appKey);
        if (existing == null) {
            return null;
        }

        SecretData secretData = existing.getData();

        try {
            byte[] plaintext = appSecrets.decryptProjectSecret(project, secretData);
            return plaintext;
        } catch (KeyczarException e) {
            throw new IllegalStateException("Error decrypting secret", e);
        }
    }

    @Override
    public ClientApp createClientApp(Auth auth, Project project, String appName, String appPassword)
            throws CloudException {
        AuthenticatedUser authenticatedUser = authService.toAuthenticatedUser(auth);

        AuthenticatedProject authenticatedProject = identityService.authenticateToProject(authenticatedUser,
                project.getId());
        if (authenticatedProject == null) {
            throw new IllegalStateException();
        }

        ClientAppSecretData secrets;
        {
            ClientAppSecretData.Builder sb = ClientAppSecretData.newBuilder();
            sb.setAppPassword(appPassword);
            secrets = sb.build();
        }

        ClientAppData existing = getStore(project.getId()).find(appName);
        if (existing != null) {
            throw new WebApplicationException(Status.CONFLICT);
        }

        ClientAppData.Builder b = ClientAppData.newBuilder();
        b.setProject(project.getId());
        b.setKey(appName);

        SecretToken secretToken = SecretToken.create(SecretTokenType.CLIENT_APP_SECRET);

        buildSecretStore(b, secretToken, appPassword, authenticatedProject);
        b.setSecretData(Secrets.buildClientAppSecret(secretToken, secrets));

        try {
            ClientAppData app = getStore(project.getId()).create(b);
            return new ClientAppImpl(app, secrets);
        } catch (DuplicateValueException e) {
            throw new WebApplicationException(Status.CONFLICT);
        }
    }

    // private String deriveKey(String name, long projectId) {
    // Hasher hasher = Hashing.md5().newHasher();
    // hasher.putString(name, Charsets.UTF_8);
    // hasher.putString("::", Charsets.UTF_8);
    // hasher.putString(Long.toString(projectId), Charsets.UTF_8);
    //
    // long id = hasher.hash().asLong();
    // return Long.toHexString(id);
    // }

    protected ClientAppSecretData findClientAppSecretData(ClientAppData app, String appPassword) {
        SecretData secretData = app.getSecretData();

        SecretToken secretToken;
        try {
            secretToken = Secrets.getSecretFromPassword(app.getSecretStore(), appPassword);
        } catch (KeyczarException e) {
            log.warn("Keyczar error while decrypting; likely bad password", e);
            return null;
        }

        if (secretToken == null) {
            return null;
        }

        ClientAppSecretData secrets;
        try {
            secrets = Secrets.unlock(secretData, secretToken, ClientAppSecretData.newBuilder());
        } catch (Exception e) {
            // Wrong password
            log.warn("Error while decrypting client app, likely wrong secret", e);
            return null;
        }

        // if (!secrets.getDeprecatedVerifyName().equals(app.getName())) {
        // log.warn("Name mismatch when verifying decrypted data");
        // return null;
        // }

        return secrets;
    }

    // protected void buildSecretData(SecretData.Builder s, ClientAppSecretData
    // secrets, String name, String password) {
    // byte[] seed = name.getBytes(Charsets.UTF_8);
    // AesKey passwordKey = KeyczarUtils.deriveKey(ITERATION_COUNT, seed,
    // password);
    // byte[] plaintext = secrets.toByteArray();
    // byte[] ciphertext = KeyczarUtils.encrypt(passwordKey, plaintext);
    //
    // s.setCiphertext(ByteString.copyFrom(ciphertext));
    // s.setVersion(1);
    // }

    void buildSecretStore(ClientAppData.Builder app, SecretToken secret, String appPassword,
            AuthenticatedProject project) {
        SecretStoreData.Builder secretStore = app.getSecretStoreBuilder();
        if (!Strings.isNullOrEmpty(appPassword)) {
            Secrets.setPassword(secretStore, appPassword, secret);
        } else {
            // Without a password, there's going to be no way to get the key
            throw new UnsupportedOperationException();
        }

        Secrets.storeLockedByProject(app.getSecretStoreBuilder(), project, secret);
    }

}
