package io.fathom.cloud.identity.api.os.resources;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.ServiceType;
import io.fathom.cloud.identity.api.os.model.User;
import io.fathom.cloud.identity.api.os.model.v2.Access;
import io.fathom.cloud.identity.api.os.model.v2.Role;
import io.fathom.cloud.identity.api.os.model.v2.Tenant;
import io.fathom.cloud.identity.api.os.model.v2.V2AuthCredentials;
import io.fathom.cloud.identity.api.os.model.v2.V2AuthRequest;
import io.fathom.cloud.identity.api.os.model.v2.V2AuthResponse;
import io.fathom.cloud.identity.api.os.model.v2.V2Endpoint;
import io.fathom.cloud.identity.api.os.model.v2.V2Service;
import io.fathom.cloud.identity.api.os.model.v2.V2Token;
import io.fathom.cloud.identity.api.os.model.v3.Endpoint;
import io.fathom.cloud.identity.api.os.model.v3.Service;
import io.fathom.cloud.identity.model.AuthenticatedUser;
import io.fathom.cloud.protobuf.CloudCommons.TokenInfo;
import io.fathom.cloud.protobuf.IdentityModel.ProjectData;
import io.fathom.cloud.protobuf.IdentityModel.RoleData;
import io.fathom.cloud.server.auth.TokenAuth;
import io.fathom.cloud.server.resources.ClientCertificate;

import java.util.List;
import java.util.Map;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.BaseEncoding;
import com.google.inject.persist.Transactional;
import com.google.protobuf.ByteString;

@Path("/openstack/identity/v2.0/tokens")
@Transactional
@Produces({ "application/json" })
public class V2TokensResource extends IdentityResourceBase {
    private static final Logger log = LoggerFactory.getLogger(V2TokensResource.class);

    @POST
    public V2AuthResponse doTokensPost(V2AuthRequest request) throws CloudException {
        V2AuthCredentials authRequest = request.authInfo;
        if (authRequest == null) {
            authRequest = new V2AuthCredentials();
        }

        boolean hasPasswordCredentials = false;
        if (authRequest.passwordCredentials != null && !Strings.isNullOrEmpty(authRequest.passwordCredentials.username)) {
            hasPasswordCredentials = true;
        }
        boolean hasChallengeResponse = false;
        if (authRequest.challengeResponse != null && !Strings.isNullOrEmpty(authRequest.challengeResponse.challenge)) {
            hasChallengeResponse = true;
        }

        ClientCertificate clientCertificate = getClientCertificate();

        if (!hasPasswordCredentials) {
            if (!hasChallengeResponse) {
                if (clientCertificate != null) {
                    // Request for challenge
                    ByteString challenge = loginService.getChallenge(clientCertificate);
                    if (challenge != null) {
                        V2AuthResponse response = new V2AuthResponse();
                        response.challenge = BaseEncoding.base64().encode(challenge.toByteArray());
                        return response;
                    }

                }
            }
        }

        AuthenticatedUser authentication = null;

        if (authRequest != null) {
            authentication = loginService.authenticate(authRequest, clientCertificate);
        }

        if (authentication == null) {
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }

        V2AuthResponse response = new V2AuthResponse();

        Access access = response.access = new Access();
        V2Token token = access.token = new V2Token();

        ProjectData project = authentication.getProject();

        // We never pass domain; we can't build a domain token with V2
        TokenInfo tokenInfo = loginService.buildTokenInfo(authentication);

        token.expires = TokenAuth.getExpiration(tokenInfo);
        token.id = tokenService.encodeToken(tokenInfo);
        if (project != null) {
            Tenant tenant = token.tenant = new Tenant();
            tenant.id = "" + project.getId();
            tenant.name = project.getName();
        }

        if (project != null) {
            List<Service> v3Services = loginService.buildServiceMap(getBaseUrl(), project);

            access.services = toV2Services(project, v3Services);
        }

        User user = access.user = new User();
        user.id = "" + authentication.getUserData().getId();
        user.name = authentication.getUserData().getName();

        if (authentication.getProjectRoleIds() != null) {
            user.roles = Lists.newArrayList();

            for (long roleId : authentication.getProjectRoleIds()) {
                RoleData role = identityService.findRole(roleId);
                if (role == null) {
                    throw new IllegalStateException();
                }

                Role model = toModel(project, role);
                user.roles.add(model);
            }
        }

        log.info("Returning auth: {}", response);

        return response;
    }

    private List<V2Service> toV2Services(ProjectData project, List<Service> v3Services) {
        List<V2Service> v2Services = Lists.newArrayList();
        for (Service v3 : v3Services) {
            V2Service v2 = new V2Service();
            v2Services.add(v2);

            v2.name = v3.name;
            v2.type = v3.type;
            v2.endpoints = toV2Endpoints(project, v3.endpoints);

            // TODO: What happened to version in V3??
            if (v2.type.equals(ServiceType.COMPUTE.getType())) {
                for (V2Endpoint endpoint : v2.endpoints) {
                    endpoint.versionId = "2";
                }
            }
        }

        return v2Services;
    }

    private List<V2Endpoint> toV2Endpoints(ProjectData project, List<Endpoint> v3Endpoints) {
        Map<String, V2Endpoint> v2Endpoints = Maps.newHashMap();

        for (Endpoint v3 : v3Endpoints) {
            String key = v3.region;

            V2Endpoint v2 = v2Endpoints.get(key);
            if (v2 == null) {
                v2 = new V2Endpoint();
                v2Endpoints.put(key, v2);

                v2.tenantId = "" + project.getId();

                v2.region = v3.region;
            }

            if (v3.interfaceName.equals("public")) {
                v2.publicURL = v3.url;
            }

            if (v3.interfaceName.equals("admin")) {
                v2.adminURL = v3.url;
            }

            if (v3.interfaceName.equals("internal")) {
                v2.internalURL = v3.url;
            }
        }

        return Lists.newArrayList(v2Endpoints.values());
    }

    private Role toModel(ProjectData project, RoleData role) {
        Role model = new Role();
        model.id = "" + role.getId();
        model.name = role.getName();
        if (project != null) {
            model.tenantId = "" + project.getId();
        }
        return model;
    }

}
