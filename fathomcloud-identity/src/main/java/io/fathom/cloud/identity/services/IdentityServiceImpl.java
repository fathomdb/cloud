package io.fathom.cloud.identity.services;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.WellKnownRoles;
import io.fathom.cloud.identity.LoginServiceImpl;
import io.fathom.cloud.identity.Users;
import io.fathom.cloud.identity.model.AuthenticatedProject;
import io.fathom.cloud.identity.model.AuthenticatedUser;
import io.fathom.cloud.identity.secrets.Migrations;
import io.fathom.cloud.identity.secrets.SecretToken;
import io.fathom.cloud.identity.secrets.SecretToken.SecretTokenType;
import io.fathom.cloud.identity.secrets.Secrets;
import io.fathom.cloud.identity.state.AuthRepository;
import io.fathom.cloud.openstack.client.identity.ChallengeResponses;
import io.fathom.cloud.protobuf.IdentityModel.CredentialData;
import io.fathom.cloud.protobuf.IdentityModel.DomainData;
import io.fathom.cloud.protobuf.IdentityModel.DomainRoles;
import io.fathom.cloud.protobuf.IdentityModel.GroupData;
import io.fathom.cloud.protobuf.IdentityModel.ProjectData;
import io.fathom.cloud.protobuf.IdentityModel.ProjectRoles;
import io.fathom.cloud.protobuf.IdentityModel.RoleData;
import io.fathom.cloud.protobuf.IdentityModel.UserData;
import io.fathom.cloud.protobuf.IdentityModel.UserSecretData;
import io.fathom.cloud.state.DuplicateValueException;
import io.fathom.cloud.state.NamedItemCollection;
import io.fathom.cloud.tasks.TaskScheduler;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.keyczar.AesKey;
import org.keyczar.DefaultKeyType;
import org.keyczar.KeyMetadata;
import org.keyczar.KeyczarKey;
import org.keyczar.KeyczarUtils;
import org.keyczar.enums.KeyPurpose;
import org.keyczar.exceptions.KeyczarException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.persist.Transactional;

@Singleton
@Transactional
public class IdentityServiceImpl implements IdentityService {
    private static final Logger log = LoggerFactory.getLogger(IdentityServiceImpl.class);

    @Inject
    AuthRepository authRepository;

    @Inject
    Secrets secretService;

    @Inject
    TaskScheduler scheduler;

    // @Override
    // public void deleteUser(long id) throws StateStoreException,
    // ConcurrentUpdateException {
    //
    // // TODO: Mark as deleted?
    // // TODO: Deleted related things e.g. credentials?
    // // TODO: Block delete if "in use"
    // authStore.getUsers().delete(id);
    // }

    @Override
    @Transactional
    public UserData createUser(UserCreationData request) throws CloudException {
        UserData.Builder user = request.user;
        request.user = null;

        String username = user.getName();
        if (Strings.isNullOrEmpty(username)) {
            throw new IllegalArgumentException();
        }

        user.setDomainId(request.domain.getId());

        String challengeKey = null;
        if (request.publicKeySha1 != null) {
            challengeKey = LoginServiceImpl.toCredentialKey(request.publicKeySha1);
        }

        if (Strings.isNullOrEmpty(request.password)) {
            request.password = null;
        }

        NamedItemCollection<CredentialData> usernameStore = authRepository.getUsernames(request.domain);

        CredentialData credential = usernameStore.find(username);
        if (credential != null) {
            throw new WebApplicationException(Status.CONFLICT);
        }

        if (challengeKey != null) {
            CredentialData publicKeyCredential = authRepository.getPublicKeyCredentials(request.domain.getId()).find(
                    challengeKey);
            if (publicKeyCredential != null) {
                throw new WebApplicationException(Status.CONFLICT);
            }
        }

        if (challengeKey == null && request.password == null) {
            // There's going to be no way to log in
            throw new IllegalArgumentException();
        }

        SecretToken userSecret;

        if (request.publicKeyChallengeRequest == null) {
            userSecret = SecretToken.create(SecretTokenType.USER_SECRET);
        } else {
            byte[] plaintext = request.publicKeyChallengeRequest.toByteArray();
            if (!ChallengeResponses.hasPrefix(plaintext)) {
                throw new IllegalArgumentException();
            }
            byte[] payload = ChallengeResponses.getPayload(plaintext);
            payload = ChallengeResponses.getPayload(payload);
            AesKey cryptoKey;
            try {
                cryptoKey = KeyczarUtils.unpack(payload);
            } catch (KeyczarException e) {
                throw new IllegalArgumentException("Invalid key", e);
            }
            userSecret = new SecretToken(SecretTokenType.USER_SECRET, cryptoKey, null);
        }

        if (request.password != null) {
            secretService.addPasswordAuth(user, userSecret, request.password);
        }

        // TODO: Allow users to opt out of password/token recovery??
        boolean allowPasswordRecovery = true;
        if (allowPasswordRecovery) {
            secretService.addTokenRecovery(user, userSecret);
        }

        if (challengeKey != null) {
            secretService.addPublicKeyAuth(user, challengeKey, request.publicKeyChallengeResponse);
        }

        KeyczarKey keypair = KeyczarUtils.createKey(new KeyMetadata("RSA Key", KeyPurpose.DECRYPT_AND_ENCRYPT,
                DefaultKeyType.RSA_PRIV));

        user.getPublicKeyBuilder().setKeyczar(KeyczarUtils.getPublicKey(keypair).toString());

        // Create the secret data
        {
            if (user.hasSecretData()) {
                throw new IllegalStateException();
            }

            UserSecretData.Builder s = UserSecretData.newBuilder();
            if (Strings.isNullOrEmpty(user.getName())) {
                throw new IllegalArgumentException();
            }
            // s.setVerifyPublicKey(Hashing.md5().hashBytes(publicKey).asLong());
            s.getPrivateKeyBuilder().setKeyczar(keypair.toString());

            user.setSecretData(Secrets.buildUserSecret(userSecret, s.build()));
        }

        UserData created = authRepository.getUsers().create(user);

        {
            CredentialData.Builder credentialBuilder = CredentialData.newBuilder();

            credentialBuilder.setUserId(created.getId());
            credentialBuilder.setKey(username);

            // if (!Strings.isNullOrEmpty(password)) {
            // PasswordHashData passwordHash = hasher.hash(password);
            // credentialBuilder.setPasswordHash(passwordHash);
            // }

            try {
                usernameStore.create(credentialBuilder);
            } catch (DuplicateValueException e) {
                // TODO: We need to be atomic! ZK supports multi, but it looks
                // complicated
                throw new WebApplicationException(Status.CONFLICT);
            }
        }

        if (challengeKey != null) {
            CredentialData.Builder b = CredentialData.newBuilder();

            b.setUserId(created.getId());
            b.setKey(challengeKey);

            try {
                authRepository.getPublicKeyCredentials(request.domain.getId()).create(b);
            } catch (DuplicateValueException e) {
                // TODO: We need to be atomic! ZK supports multi, but it looks
                // complicated
                throw new WebApplicationException(Status.CONFLICT);
            }
        }

        return created;

    }

    @Override
    public DomainData getDefaultDomain() throws CloudException {
        return authRepository.getDomains().getDefaultDomain();
    }

    @Override
    public ProjectData createProject(ProjectData.Builder b, AuthenticatedUser owner, long ownerRoleId)
            throws CloudException {
        // TODO: Policy that only domain admins can create projects?
        // Don't see why!

        // Auth.Domain domain = findDomainWithAdminRole();
        // if (domain == null) {
        // throw new WebApplicationException(Status.FORBIDDEN);
        // }

        ProjectData created = authRepository.getProjects().create(b);

        SecretToken secretToken = SecretToken.create(SecretTokenType.PROJECT_SECRET);

        AuthenticatedProject authenticatedProject = new AuthenticatedProject(created, secretToken);

        grantRoleToUserOnProject(authenticatedProject, owner.getUserId(), ownerRoleId);

        return created;
    }

    @Override
    @Transactional
    public void grantRoleToUserOnProject(AuthenticatedProject authenticatedProject, long granteeUserId, long roleId)
            throws CloudException {
        RoleData role = authRepository.getRoles().find(roleId);
        if (role == null) {
            throw new IllegalArgumentException("Cannot find role");
        }

        long projectId = authenticatedProject.getProjectId();

        UserData granteeData = authRepository.getUsers().find(granteeUserId);
        if (granteeData == null) {
            throw new IllegalArgumentException();
        }

        UserData.Builder b = UserData.newBuilder(granteeData);

        {
            ProjectRoles.Builder pb = null;
            for (ProjectRoles.Builder i : b.getProjectRolesBuilderList()) {
                if (i.getProject() == projectId) {
                    pb = i;
                    break;
                }
            }

            if (pb == null) {
                pb = b.addProjectRolesBuilder();
                pb.setProject(projectId);
            }

            if (!pb.hasSecretData()) {
                try {
                    pb.setSecretData(Secrets.buildProjectRolesSecret(granteeData, authenticatedProject));
                } catch (KeyczarException e) {
                    throw new CloudException("Crypto error granting project role", e);
                }
            }

            if (!pb.getRoleList().contains(role.getId())) {
                pb.addRole(role.getId());
            }

            authRepository.getUsers().update(b);
        }
    }

    @Override
    @Transactional
    public void grantDomainRoleToUser(long domainId, long granteeUserId, long roleId) throws CloudException {
        RoleData role = authRepository.getRoles().find(roleId);
        if (role == null) {
            throw new IllegalArgumentException("Cannot find role");
        }

        UserData granteeData = authRepository.getUsers().find(granteeUserId);
        if (granteeData == null) {
            throw new IllegalArgumentException();
        }

        DomainData domain = authRepository.getDomains().find(domainId);
        if (domain == null) {
            throw new IllegalArgumentException();
        }

        UserData.Builder b = UserData.newBuilder(granteeData);

        {
            DomainRoles.Builder rb = null;
            for (DomainRoles.Builder i : b.getDomainRolesBuilderList()) {
                if (i.getDomain() == domainId) {
                    rb = i;
                    break;
                }
            }

            if (rb == null) {
                rb = b.addDomainRolesBuilder();
                rb.setDomain(domainId);
            }

            if (!rb.getRoleList().contains(role.getId())) {
                rb.addRole(role.getId());
            }

            authRepository.getUsers().update(b);
        }
    }

    @Override
    public ProjectData findProject(AuthenticatedUser user, long projectId) throws CloudException {
        ProjectData project = authRepository.getProjects().find(projectId);
        boolean authorized = false;

        if (project != null) {
            ProjectRoles projectRoles = Users.findProjectRoles(user.getUserData(), project.getId());
            if (projectRoles != null && projectRoles.getRoleCount() != 0) {
                authorized = true;
            }

            if (!authorized) {
                if (user.isDomainAdmin(project.getDomainId())) {
                    authorized = true;
                }
            }
        }

        if (!authorized) {
            log.info("User {} not authorized on project {}", user, project);
            project = null;
        }

        return project;
    }

    @Override
    public void deleteUser(UserData user) throws CloudException {
        // TODO: Mark as deleted?
        // TODO: Deleted related things e.g. credentials?
        // TODO: Block delete if "in use"
        authRepository.getUsers().delete(user.getId());
    }

    @Inject
    Provider<Sweeper> sweeper;

    @Override
    public void sweep() throws CloudException {
        sweeper.get().sweep();
    }

    @Override
    public RoleData findRole(long roleId) {
        return authRepository.getRoles().find(roleId);
    }

    @Override
    public List<RoleData> listRoles() {
        return authRepository.getRoles().list();
    }

    @Override
    public void fixupProject(AuthenticatedUser user, long projectId) throws CloudException {
        ProjectData project = findProject(user, projectId);
        if (project == null) {
            log.warn("Could not find project");
            return;
        }

        ProjectRoles projectRoles = Users.findProjectRoles(user.getUserData(), project.getId());
        if (projectRoles == null) {
            log.warn("Could not find role on project");
            // TODO: We probably need another path for domain admins
            return;
        }

        if (!projectRoles.hasSecretData()) {
            log.warn("Project role has no secret data");

            if (projectRoles.getRoleList().contains(WellKnownRoles.ROLE_ID_ADMIN)) {
                // TODO: Remove once we've migrated all the projects
                log.warn("Creating project key for project: {}", projectId);

                Migrations.report(project);

                AesKey cryptoKey = KeyczarUtils.generateSymmetricKey();

                long userId = user.getUserId();

                SecretToken secretToken = new SecretToken(SecretTokenType.PROJECT_SECRET, cryptoKey, null);
                AuthenticatedProject authenticatedProject = new AuthenticatedProject(project, secretToken);

                grantRoleToUserOnProject(authenticatedProject, userId, WellKnownRoles.ROLE_ID_ADMIN);
            } else {
                log.warn("User is not admin, cannot create secret");
            }
        }
    }

    @Override
    public AuthenticatedProject authenticateToProject(AuthenticatedUser user, long projectId) throws CloudException {
        ProjectData project = findProject(user, projectId);
        if (project == null) {
            return null;
        }

        AuthenticatedProject authenticatedProject = secretService.authenticate(project, user);
        if (authenticatedProject == null) {
            return null;
        }

        return authenticatedProject;
    }

    @Override
    public UserData findUser(long userId) throws CloudException {
        return authRepository.getUsers().find(userId);
    }

    @Override
    public void start() throws CloudException {
        // TODO: Just support method annotations??
        scheduler.schedule(SweepTask.class);
    }

    @Override
    public List<DomainData> listDomains(UserData user) throws CloudException {
        List<DomainData> ret = Lists.newArrayList();
        for (DomainData domain : authRepository.getDomains().list()) {
            // TODO: Other domains?
            if (domain.getId() != user.getDomainId()) {
                continue;
            }
            ret.add(domain);
        }
        return ret;
    }

    @Override
    public DomainData findDomain(UserData user, String id) throws CloudException {
        DomainData domain = authRepository.getDomains().find(Long.valueOf(id));

        // TODO: Other domains?
        if (domain != null && domain.getId() != user.getDomainId()) {
            domain = null;
        }

        return domain;
    }

    @Override
    public List<GroupData> listGroups(AuthenticatedUser user) throws CloudException {
        long domainId = user.getDomainId();
        boolean isAdmin = user.isDomainAdmin(domainId);

        Set<Long> userGroups = Sets.newHashSet(user.getUserData().getGroupsList());

        List<GroupData> ret = Lists.newArrayList();
        for (GroupData group : authRepository.getGroups(domainId).list()) {
            if (!isAdmin) {
                if (!userGroups.contains(group.getId())) {
                    continue;
                }
            }
            ret.add(group);
        }

        return ret;
    }

    @Override
    public UserData findUserByName(long domainId, String userName) throws CloudException {
        DomainData domain = authRepository.getDomains().find(domainId);
        if (domain == null) {
            throw new IllegalArgumentException();
        }

        CredentialData credentialData = authRepository.getUsernames(domain).find(userName);
        if (credentialData == null) {
            return null;
        }

        long userId = credentialData.getUserId();
        UserData user = authRepository.getUsers().find(userId);

        if (user == null) {
            // Unexpected!
            log.warn("Unable to find user for credential: {}", userName);
        }

        return user;

    }

    @Override
    public List<ProjectData> listProjects() throws CloudException {
        return authRepository.getProjects().list();
    }
}
