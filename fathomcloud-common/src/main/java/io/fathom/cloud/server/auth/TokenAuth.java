package io.fathom.cloud.server.auth;

import io.fathom.cloud.WellKnownRoles;
import io.fathom.cloud.protobuf.CloudCommons.TokenInfo;
import io.fathom.cloud.protobuf.CloudCommons.TokenScope;
import io.fathom.cloud.server.model.Project;
import io.fathom.cloud.server.model.User;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public class TokenAuth implements Auth {
    private static final Logger log = LoggerFactory.getLogger(TokenAuth.class);

    private final TokenInfo token;

    public TokenAuth(TokenInfo token) {
        Preconditions.checkNotNull(token);
        this.token = token;

        if (hasExpired(token)) {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public Auth.Domain findDomainWithAdminRole() {
        if (getTokenScope() == TokenScope.Domain) {
            long domainId = token.getDomainId();
            if (domainId == 0) {
                throw new IllegalStateException();
            }

            if (!isInDomainRole(WellKnownRoles.ROLE_ID_ADMIN)) {
                return null;
            }

            return new DomainImpl(domainId);
        } else {
            log.warn("Using compatible behaviour; accepting non-domain token for domain action");
            long domainId = token.getDomainId();
            if (domainId == 0) {
                log.warn("No domain id set in token: {}", token);
                return null;
            }

            if (!isInDomainRole(WellKnownRoles.ROLE_ID_ADMIN)) {
                return null;
            }

            return new DomainImpl(domainId);
        }
    }

    private boolean isInRole(long roleId) {
        for (int i = 0; i < token.getRolesCount(); i++) {
            if (roleId == token.getRoles(i)) {
                return true;
            }
        }
        return false;
    }

    private boolean isInDomainRole(long roleId) {
        for (int i = 0; i < token.getDomainRolesCount(); i++) {
            if (roleId == token.getDomainRoles(i)) {
                return true;
            }
        }
        return false;
    }

    static class DomainImpl implements Domain {
        final long id;

        public DomainImpl(long id) {
            this.id = id;
        }

        @Override
        public long getId() {
            return id;
        }

    }

    public TokenScope getTokenScope() {
        return token.getTokenScope();
    }

    @Override
    public boolean checkProject(long projectId) {
        switch (getTokenScope()) {
        case Domain: {
            // Domain domainAdmin = findDomainAdmin();
            // if (domainAdmin != null) {
            // return false;
            // }
            log.warn("Attempt to use domain token for project access: not supported");
            throw new UnsupportedOperationException();
        }

        case Project: {
            return token.getProjectId() == projectId;
        }

        case Unscoped:
            log.debug("Attempt to use unscoped token for project access: denied");
            return false;
        }

        throw new IllegalStateException();
    }

    public static boolean hasExpired(TokenInfo tokenInfo) {
        if (!tokenInfo.hasExpiration()) {
            if (isServiceToken(tokenInfo)) {
                // Service tokens are allowed not to expire
                return false;
            } else {
                log.warn("Token did not have expiration time; defensively treating as expired");
                return true;
            }
        }

        long expiration = tokenInfo.getExpiration();
        long now = System.currentTimeMillis() / 1000L;

        return expiration < now;
    }

    private static boolean isServiceToken(TokenInfo tokenInfo) {
        return tokenInfo.getServiceToken();
    }

    @Override
    public User getUser() {
        if (token.hasUserId()) {
            return new User(token.getUserId());
        }
        return null;
    }

    @Override
    public Project getProject() {
        if (token.hasProjectId()) {
            return new Project(token.getProjectId());
        }
        return null;
    }

    public TokenInfo getTokenInfo() {
        return token;
    }

    public static Date getExpiration(TokenInfo tokenInfo) {
        long expiration = tokenInfo.getExpiration();
        expiration *= 1000L;
        return new Date(expiration);
    }

    @Override
    public String toString() {
        return "TokenAuth [userId=" + token.getUserId() + "]";
    }

}
