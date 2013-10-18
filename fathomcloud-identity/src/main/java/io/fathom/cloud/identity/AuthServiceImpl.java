package io.fathom.cloud.identity;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.WellKnownRoles;
import io.fathom.cloud.identity.model.AuthenticatedUser;
import io.fathom.cloud.identity.secrets.Secrets;
import io.fathom.cloud.identity.services.IdentityService;
import io.fathom.cloud.identity.state.AuthRepository;
import io.fathom.cloud.protobuf.CloudCommons.TokenInfo;
import io.fathom.cloud.protobuf.CloudCommons.TokenScope;
import io.fathom.cloud.protobuf.IdentityModel.ProjectData;
import io.fathom.cloud.protobuf.IdentityModel.ProjectRoles;
import io.fathom.cloud.protobuf.IdentityModel.UserData;
import io.fathom.cloud.server.auth.Auth;
import io.fathom.cloud.server.auth.SharedSecretTokenService;
import io.fathom.cloud.server.auth.TokenAuth;
import io.fathom.cloud.server.model.Project;
import io.fathom.cloud.services.AuthService;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.inject.persist.Transactional;
import com.google.protobuf.ByteString;

@Singleton
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    @Inject
    LoginService loginService;

    @Inject
    AuthRepository authRepository;

    @Inject
    SharedSecretTokenService tokenService;

    @Inject
    Secrets secretService;

    @Inject
    IdentityService identityService;

    @Override
    @Transactional
    public Auth authenticate(Long projectId, String username, String password) throws CloudException {
        AuthenticatedUser authentication = loginService.authenticate(projectId, username, password);

        if (authentication == null) {
            return null;
        }

        TokenInfo tokenInfo = loginService.buildTokenInfo(authentication);
        return new TokenAuth(tokenInfo);
    }

    @Override
    @Transactional
    public List<Long> resolveProjectName(Auth auth, String projectName) throws CloudException {
        UserData userData = authRepository.getUsers().find(auth.getUser().getId());
        if (userData == null) {
            log.warn("Unable to find user: {}", auth.getUser());
            return null;
        }

        List<Long> projectIds = Lists.newArrayList();

        for (ProjectRoles pr : userData.getProjectRolesList()) {
            long projectId = pr.getProject();

            ProjectData project = authRepository.getProjects().find(projectId);
            if (project == null) {
                log.warn("Unable to find project: {}", projectId);
                continue;
            }

            if (projectName.equals(project.getName())) {
                projectIds.add(projectId);
            }
        }
        return projectIds;
    }

    @Override
    @Transactional
    public String createServiceToken(Auth auth, long instanceId) throws CloudException {
        long projectId = auth.getProject().getId();
        if (projectId == 0) {
            throw new IllegalStateException();
        }

        ProjectData project = authRepository.getProjects().find(projectId);
        if (project == null) {
            log.warn("Unable to find project: {}", auth.getProject());
            return null;
        }

        TokenInfo.Builder b = TokenInfo.newBuilder();

        b.setTokenScope(TokenScope.Project);

        b.setDomainId(project.getDomainId());
        b.setProjectId(projectId);

        // TODO: Same roles as auth??
        b.addRoles(WellKnownRoles.ROLE_ID_MEMBER);

        {
            AuthenticatedUser authenticatedUser = toAuthenticatedUser(auth);

            ByteString tokenSecret = secretService.buildTokenSecret(authenticatedUser);
            b.setTokenSecret(tokenSecret);

            // For now, we can't get the secret without going through a user
            // For now, we use the user's id. It provides some degree of
            // auditing.
            log.warn("Creating service token using user's credentials");

            b.setUserId(auth.getUser().getId());
        }

        b.setServiceToken(true);
        b.setInstanceId(instanceId);

        String tokenId = tokenService.encodeToken(b.build());
        return tokenId;
    }

    @Override
    public String getIdentityUri(String baseUrl) {
        return baseUrl + "/v2.0";
    }

    @Override
    public Long createProject(Auth auth, String projectName) throws CloudException {
        Auth.Domain domain = auth.findDomainWithAdminRole();
        if (domain == null) {
            throw new WebApplicationException(Status.FORBIDDEN);
        }

        ProjectData.Builder b = ProjectData.newBuilder();

        // if (!Strings.isNullOrEmpty(req.description)) {
        // b.setDescription(req.description);
        // }

        b.setName(projectName);

        b.setDomainId(domain.getId());

        b.setEnabled(true);

        AuthenticatedUser owner = toAuthenticatedUser(auth);

        ProjectData created = identityService.createProject(b, owner, WellKnownRoles.ROLE_ID_ADMIN);

        return created.getId();
    }

    public AuthenticatedUser toAuthenticatedUser(Auth auth) throws CloudException {
        TokenAuth tokenAuth = (TokenAuth) auth;
        TokenInfo tokenInfo = tokenAuth.getTokenInfo();
        AuthenticatedUser authenticatedUser = loginService.authenticate(tokenInfo);
        return authenticatedUser;
    }

    @Override
    public Project findSystemProject() throws CloudException {
        log.warn("findSystemProject is very inefficient");

        for (ProjectData project : authRepository.getProjects().list()) {
            if (project.getName().equals(SYSTEM_PROJECT_NAME)) {
                return new Project(project.getId());
            }
        }
        return null;
    }
}
