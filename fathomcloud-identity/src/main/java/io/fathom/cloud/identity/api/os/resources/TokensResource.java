package io.fathom.cloud.identity.api.os.resources;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.identity.api.os.model.v3.Domain;
import io.fathom.cloud.identity.api.os.model.v3.Token;
import io.fathom.cloud.identity.api.os.model.v3.Token.ProjectInfo;
import io.fathom.cloud.identity.api.os.model.v3.Token.RoleInfo;
import io.fathom.cloud.identity.api.os.model.v3.Token.UserInfo;
import io.fathom.cloud.identity.model.AuthenticatedUser;
import io.fathom.cloud.identity.services.TokenUtils;
import io.fathom.cloud.identity.state.AuthRepository;
import io.fathom.cloud.protobuf.CloudCommons.TokenInfo;
import io.fathom.cloud.protobuf.IdentityModel.DomainData;
import io.fathom.cloud.protobuf.IdentityModel.ProjectData;
import io.fathom.cloud.protobuf.IdentityModel.RoleData;
import io.fathom.cloud.protobuf.IdentityModel.UserData;
import io.fathom.cloud.server.auth.TokenAuth;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.inject.persist.Transactional;

@Path("/openstack/identity/v3/auth/tokens")
@Transactional
@Produces({ "application/json" })
public class TokensResource extends IdentityResourceBase {
    private static final Logger log = LoggerFactory.getLogger(TokensResource.class);

    // Should use identity service instead
    @Deprecated
    @Inject
    AuthRepository authRepository;

    @Inject
    TokenUtils tokenUtils;

    @GET
    @Produces({ JSON })
    public Response getTokenInfo(@HeaderParam("x-auth-token") String authToken,
            @HeaderParam("x-subject-token") String subjectToken) throws CloudException {
        if (Strings.isNullOrEmpty(authToken) || Strings.isNullOrEmpty(subjectToken)) {
            throw new WebApplicationException(Status.BAD_REQUEST);
        }

        if (!authToken.equals(subjectToken)) {
            // For now, we only allow same-token auth
            throw new UnsupportedOperationException();
        }

        AuthenticatedUser auth = loginService.authenticate(authToken);
        if (auth == null) {
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }

        TokenInfo subject = tokenService.findValidToken(subjectToken);
        if (subject == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        ResponseBuilder response = Response.ok();
        response.entity(buildTokenModel(subject));
        return response.build();
    }

    private Token buildTokenModel(TokenInfo token) throws CloudException {
        Preconditions.checkNotNull(token);

        Token response = new Token();

        response.expires = TokenAuth.getExpiration(token);

        UserData user = authRepository.getUsers().find(token.getUserId());
        if (user == null) {
            throw new IllegalStateException();
        }

        DomainData domain = authRepository.getDomains().find(user.getDomainId());
        if (domain == null) {
            throw new IllegalStateException();
        }

        response.user = toModel(domain, user);

        List<RoleData> roles = null;

        switch (token.getTokenScope()) {
        case Domain: {
            // Domain scoped
            DomainData scope = authRepository.getDomains().find(token.getDomainId());
            if (scope == null) {
                throw new IllegalStateException();
            }

            response.domainScope = toModel(domain);

            response.serviceCatalog = loginService.buildServiceMap(getBaseUrl(), null);
            break;
        }

        case Project: {
            // Project scoped
            ProjectData scope = authRepository.getProjects().find(token.getProjectId());
            if (scope == null) {
                throw new IllegalStateException();
            }

            DomainData projectDomain = authRepository.getDomains().find(scope.getDomainId());
            if (projectDomain == null) {
                throw new IllegalStateException();
            }

            response.projectScope = toModel(projectDomain, scope);
            response.serviceCatalog = loginService.buildServiceMap(getBaseUrl(), scope);

            roles = tokenUtils.getProjectRoles(user, scope.getId());
            break;
        }

        default:
            break;
        }

        if (roles != null) {
            response.roles = Lists.newArrayList();

            for (RoleData role : roles) {
                RoleInfo model = toModel(role);
                response.roles.add(model);
            }
        }

        return response;

    }

    private RoleInfo toModel(RoleData role) {
        RoleInfo model = new RoleInfo();
        model.id = Long.toString(role.getId());
        model.name = role.getName();
        return model;
    }

    private UserInfo toModel(DomainData domain, UserData user) {
        UserInfo model = new UserInfo();
        model.id = Long.toString(user.getId());
        model.name = user.getName();
        model.domain = toModel(domain);
        return model;
    }

    private Domain toModel(DomainData domain) {
        Domain model = new Domain();
        model.id = Long.toString(domain.getId());
        model.name = domain.getName();
        return model;
    }

    private ProjectInfo toModel(DomainData domain, ProjectData project) {
        ProjectInfo model = new ProjectInfo();
        model.id = Long.toString(project.getId());
        model.name = project.getName();
        model.domain = toModel(domain);
        return model;
    }
}
