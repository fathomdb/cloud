package io.fathom.cloud.identity.api.os.resources;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.identity.LoginService;
import io.fathom.cloud.identity.api.os.model.Project;
import io.fathom.cloud.identity.model.AuthenticatedUser;
import io.fathom.cloud.identity.secrets.Secrets;
import io.fathom.cloud.identity.services.IdentityService;
import io.fathom.cloud.protobuf.CloudCommons.TokenInfo;
import io.fathom.cloud.protobuf.IdentityModel.ProjectData;
import io.fathom.cloud.protobuf.IdentityModel.UserData;
import io.fathom.cloud.server.auth.Auth;
import io.fathom.cloud.server.auth.SharedSecretTokenService;
import io.fathom.cloud.server.auth.TokenAuth;
import io.fathom.cloud.server.model.User;
import io.fathom.cloud.server.resources.OpenstackResourceBase;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class IdentityResourceBase extends OpenstackResourceBase {
    private static final Logger log = LoggerFactory.getLogger(IdentityResourceBase.class);

    // @Inject
    // protected AuthRepository authRepository;

    @Inject
    protected SharedSecretTokenService tokenService;

    @Inject
    protected LoginService loginService;

    @Inject
    protected Secrets secretService;

    @Inject
    protected IdentityService identityService;

    protected ProjectData getProject(long projectId) throws CloudException {
        ProjectData project = identityService.findProject(getAuthenticatedUser(), projectId);
        if (project == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }
        return project;
    }

    // protected DomainData requireDomainAdmin() {
    // Auth auth = findAuth();
    // if (auth == null) {
    // throw new WebApplicationException(Status.FORBIDDEN);
    // }
    //
    // Auth.Domain domain = auth.findDomainAdmin();
    // if (domain == null) {
    // throw new WebApplicationException(Status.FORBIDDEN);
    // }
    //
    // return domain;
    // }

    protected Project toModel(ProjectData data) {
        Project p = new Project();

        p.id = "" + data.getId();
        p.name = data.getName();
        p.description = data.getDescription();
        p.enabled = true;

        return p;
    }

    protected TokenInfo findTokenInfo() throws CloudException {
        Auth auth = findAuth();
        if (auth == null) {
            return null;
        }

        if (auth instanceof TokenAuth) {
            TokenAuth tokenAuth = (TokenAuth) auth;

            TokenInfo tokenInfo = tokenAuth.getTokenInfo();
            return tokenInfo;
        } else {
            throw new IllegalArgumentException();
        }
    }

    // private DomainData domain;
    //
    // protected DomainData findDomainFromToken() throws CloudException {
    // if (domain == null) {
    // TokenInfo tokenInfo = findTokenInfo();
    //
    // this.domain = findDomainFromToken(tokenInfo);
    // // authStore.getUsers().find(userId);
    // }
    // return domain;
    // }

    // protected DomainData findDomainFromToken(TokenInfo tokenInfo) throws
    // CloudException {
    // if (tokenInfo == null) {
    // return null;
    // }
    //
    // long domainId = -1;
    // if (tokenInfo.hasDomainId()) {
    // domainId = tokenInfo.getDomainId();
    // } else if (tokenInfo.hasProjectId()) {
    // long projectId = tokenInfo.getProjectId();
    // ProjectData project = authRepository.getProjects().find(projectId);
    // if (project != null) {
    // domainId = project.getDomainId();
    // }
    // } else {
    // // throw new UnsupportedOperationException();
    // }
    //
    // if (domainId >= 0) {
    // return authRepository.getDomains().find(domainId);
    // } else {
    // return null;
    // }
    // }

    private UserData user = null;

    protected UserData getUser() throws CloudException {
        if (this.user == null) {
            Auth auth = getAuth();
            User user = null;
            if (auth != null) {
                user = auth.getUser();
            }
            if (user == null) {
                throw new WebApplicationException(Status.UNAUTHORIZED);
            }

            this.user = identityService.findUser(user.getId());
        }
        return this.user;
    }

    private AuthenticatedUser authenticatedUser = null;

    protected AuthenticatedUser findAuthenticatedUser() throws CloudException {
        if (this.authenticatedUser == null) {
            TokenAuth auth = (TokenAuth) getAuth();
            TokenInfo tokenInfo = auth.getTokenInfo();
            this.authenticatedUser = loginService.authenticate(tokenInfo);
        }
        return this.authenticatedUser;
    }

    protected AuthenticatedUser getAuthenticatedUser() throws CloudException {
        AuthenticatedUser user = findAuthenticatedUser();
        if (user == null) {
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }
        return user;
    }

    protected UserData getUser(long userId) throws CloudException {
        UserData user = findUser(userId);
        if (user == null) {
            log.info("User not found / authorized: {}", userId);
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        return user;
    }

    protected UserData findUser(long id) throws CloudException {
        UserData user = getUser();
        if (user.getId() == id) {
            return user;
        }

        Auth.Domain domainAdmin = findDomainWithAdminRole();
        UserData otherUser = null;
        if (domainAdmin != null) {
            otherUser = identityService.findUser(id);

            if (otherUser != null) {
                if (otherUser.getDomainId() != domainAdmin.getId()) {
                    otherUser = null;
                }
            }
        }

        return otherUser;
    }

    // protected DomainData getDomainFromToken() throws CloudException {
    // DomainData domain = findDomainFromToken();
    // if (domain == null) {
    // throw new WebApplicationException(Status.UNAUTHORIZED);
    // }
    // return domain;
    // }

    // protected DomainData getDomain(long domainId) throws CloudException {
    // DomainData domain = getDomainFromToken();
    // if (domain.getId() != domainId) {
    // throw new WebApplicationException(Status.UNAUTHORIZED);
    // }
    // return domain;
    // }

    protected Auth.Domain findDomainWithAdminRole() {
        Auth auth = findAuth();
        if (auth == null) {
            return null;
        }
        return auth.findDomainWithAdminRole();
    }

}
