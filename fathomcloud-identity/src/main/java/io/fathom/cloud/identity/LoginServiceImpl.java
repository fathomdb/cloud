package io.fathom.cloud.identity;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.OpenstackExtension;
import io.fathom.cloud.ServiceType;
import io.fathom.cloud.identity.api.os.model.v2.V2AuthCredentials;
import io.fathom.cloud.identity.api.os.model.v3.Endpoint;
import io.fathom.cloud.identity.api.os.model.v3.Service;
import io.fathom.cloud.identity.model.AuthenticatedUser;
import io.fathom.cloud.identity.secrets.Secrets;
import io.fathom.cloud.identity.secrets.UserWithSecret;
import io.fathom.cloud.identity.services.IdentityService;
import io.fathom.cloud.identity.state.AuthRepository;
import io.fathom.cloud.openstack.client.identity.ChallengeResponses;
import io.fathom.cloud.protobuf.CloudCommons.TokenInfo;
import io.fathom.cloud.protobuf.CloudCommons.TokenScope;
import io.fathom.cloud.protobuf.IdentityModel.CredentialData;
import io.fathom.cloud.protobuf.IdentityModel.DomainData;
import io.fathom.cloud.protobuf.IdentityModel.ProjectData;
import io.fathom.cloud.protobuf.IdentityModel.ProjectRoles;
import io.fathom.cloud.protobuf.IdentityModel.UserData;
import io.fathom.cloud.server.auth.TokenAuth;
import io.fathom.cloud.server.auth.TokenService;
import io.fathom.cloud.server.model.Project;
import io.fathom.cloud.server.resources.ClientCertificate;
import io.fathom.cloud.server.resources.OpenstackDefaults;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.keyczar.AesKey;
import org.keyczar.KeyczarUtils;
import org.keyczar.exceptions.KeyczarException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathomdb.TimeSpan;
import com.fathomdb.extensions.ExtensionModule;
import com.fathomdb.extensions.Extensions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.io.BaseEncoding;
import com.google.inject.persist.Transactional;
import com.google.protobuf.ByteString;

@Singleton
public class LoginServiceImpl implements LoginService {
    private static final Logger log = LoggerFactory.getLogger(LoginServiceImpl.class);

    protected static final TimeSpan TOKEN_VALIDITY = new TimeSpan("1h");

    @Inject
    Secrets secretService;

    @Inject
    AuthRepository authRepository;

    @Inject
    TokenService tokenService;

    @Inject
    IdentityService identityService;

    @Inject
    Extensions extensions;

    @Override
    public List<Service> buildServiceMap(String baseUrl, ProjectData project) {
        List<Service> services = Lists.newArrayList();

        // {
        // Service service = new Service();
        // services.add(service);
        //
        // service.id = service.name = "keystone";
        // service.type = ServiceType.IDENTITY.getType();
        //
        // addEndpoint(service, baseUrl + "/openstack/identity/v2.0");
        // }

        if (project != null) {
            Service service = new Service();
            services.add(service);

            service.id = service.name = "nova";
            service.type = ServiceType.COMPUTE.getType();

            addEndpoint(service, baseUrl + "/openstack/compute/" + project.getId());
        }

        if (project != null) {
            Service service = new Service();
            services.add(service);

            service.id = service.name = "trove";
            service.type = ServiceType.DBAAS.getType();

            addEndpoint(service, baseUrl + "/openstack/dbaas/" + project.getId());
        }

        // if (project != null) {
        // Service service = new Service();
        // services.add(service);
        //
        // service.id = service.name = "heat";
        // service.type = ServiceTypes.ORCHESTRATION;
        //
        // addEndpoint(service, baseUrl + "/openstack/orchestration/" +
        // project.getId());
        // }

        {
            Service service = new Service();
            services.add(service);

            service.name = "glance";
            service.type = ServiceType.IMAGE.getType();

            addEndpoint(service, baseUrl + "/openstack/images");
        }

        if (project != null) {
            Service service = new Service();
            services.add(service);

            service.name = "swift";
            service.type = ServiceType.OBJECT_STORE.getType();

            addEndpoint(service, baseUrl + "/openstack/storage/" + project.getId());
        }

        List<ServiceType> enabledServices = Lists.newArrayList();

        Project genericProject = new Project(project.getId());
        for (ExtensionModule extension : extensions.getExtensions()) {
            if (extension instanceof OpenstackExtension) {
                List<ServiceType> extensionServices = ((OpenstackExtension) extension).getServices(genericProject,
                        baseUrl);
                enabledServices.addAll(extensionServices);
            }
        }

        for (ServiceType serviceType : enabledServices) {
            Service service = new Service();
            services.add(service);

            service.name = serviceType.getName();
            service.id = service.name;
            service.type = serviceType.getType();

            String url = baseUrl + "/openstack/" + serviceType.getUrl();
            if (serviceType != ServiceType.IDENTITY) {
                // TODO: Transform to attribute on ServiceType?
                // TODO: Do we need 'v2.0' on identity endpoint?
                url += "/" + project.getId();
            }

            addEndpoint(service, url);
        }

        return services;

    }

    private void addEndpoint(Service service, String url) {

        if (service.endpoints == null) {
            service.endpoints = Lists.newArrayList();
        }

        for (String interfaceName : new String[] { "public", "internal", "admin" }) {
            Endpoint endpoint = new Endpoint();
            service.endpoints.add(endpoint);

            endpoint.id = service.id + "-" + interfaceName;
            endpoint.name = service.name;
            endpoint.interfaceName = interfaceName;
            endpoint.region = OpenstackDefaults.DEFAULT_REGION;
            endpoint.serviceId = service.id;
            endpoint.url = url;

            // Link link = new Link();
            // link.self = url;
            // endpoint.links.add(link);
        }
    }

    @Override
    @Transactional
    public AuthenticatedUser authenticate(V2AuthCredentials authRequest, ClientCertificate clientCertificate)
            throws CloudException {
        DomainData domain = null;
        UserWithSecret userWithSecret = null;

        log.info("V2 Auth request: " + authRequest);

        ProjectSpec projectSpec = new ProjectSpec();
        if (!Strings.isNullOrEmpty(authRequest.tenantId)) {
            projectSpec.projectId = Long.valueOf(authRequest.tenantId);
        }

        projectSpec.projectName = authRequest.tenantName;

        if (authRequest.passwordCredentials != null) {
            domain = identityService.getDefaultDomain();
            userWithSecret = authenticate(domain, authRequest.passwordCredentials.username,
                    authRequest.passwordCredentials.password);
        } else if (authRequest.tokenCredentials != null) {
            String tokenId = authRequest.tokenCredentials.id;

            TokenInfo tokenInfo = findTokenInfo(tokenId);
            if (tokenInfo == null) {
                return null;
            }

            userWithSecret = checkSecret(tokenInfo);

            domain = authRepository.getDomains().find(tokenInfo.getDomainId());
            if (domain == null) {
                throw new IllegalStateException();
            }

            if (projectSpec.projectId == 0 && projectSpec.projectName == null) {
                log.info("Token login with scope: {}", tokenInfo.getTokenScope());

                if (tokenInfo.getTokenScope() == TokenScope.Project) {
                    projectSpec.projectId = tokenInfo.getProjectId();
                    log.info("Set projectId to: {}", projectSpec.projectId);
                }
            }
            // This is weird, but valid with V3's deprecation of unscoped
            // tokens...
            // if (tokenInfo.getTokenScope() != TokenScope.Unscoped) {
            // log.warn("Use of scoped token in authenticate request: {} with {}",
            // authRequest, tokenInfo);
            // throw new IllegalStateException();
            // }

            // if (tokenInfo.hasProjectId()) {
            // projectSpec.projectId = tokenInfo.getProjectId();
            // projectSpec.projectName = null;
            // } else {
            // // Not sure what to do here..
            // throw new UnsupportedOperationException();
            // }
        } else if (authRequest.challengeResponse != null && clientCertificate != null) {
            domain = identityService.getDefaultDomain();

            ByteString response = ByteString.copyFrom(BaseEncoding.base64().decode(
                    authRequest.challengeResponse.response));
            ByteString challenge = ByteString.copyFrom(BaseEncoding.base64().decode(
                    authRequest.challengeResponse.challenge));
            userWithSecret = authenticate(domain, clientCertificate, challenge, response);
        }

        if (userWithSecret == null) {
            return null;
        }

        AuthenticatedUser user = toAuthenticationV2(domain, projectSpec, userWithSecret);
        return user;
    }

    @Override
    @Transactional
    public AuthenticatedUser authenticate(String tokenId) throws CloudException {
        TokenInfo tokenInfo = findTokenInfo(tokenId);
        if (tokenInfo == null) {
            return null;
        }

        return authenticate(tokenInfo);
    }

    @Override
    @Transactional
    public AuthenticatedUser authenticate(TokenInfo tokenInfo) throws CloudException {
        DomainData domain = findDomainFromToken(tokenInfo);

        UserWithSecret userWithSecret = checkSecret(tokenInfo);
        if (userWithSecret == null) {
            return null;
        }

        TokenScope scope = tokenInfo.getTokenScope();

        switch (scope) {
        case Domain:
            return buildDomainToken(domain, userWithSecret);

        case Project:
            return buildProjectToken(domain, tokenInfo.getProjectId(), userWithSecret);

        case Unscoped:
            throw new UnsupportedOperationException();

        default:
            throw new IllegalStateException();
        }
    }

    private UserWithSecret checkSecret(TokenInfo tokenInfo) throws CloudException {
        if (TokenAuth.hasExpired(tokenInfo)) {
            // This is treated the same as an invalid token
            return null;
        }

        UserData userData = authRepository.getUsers().find(tokenInfo.getUserId());
        if (userData == null) {
            return null;
        }

        UserWithSecret userWithSecret = null;
        try {
            userWithSecret = secretService.getFromToken(userData, tokenInfo);
        } catch (KeyczarException e) {
            log.info("Error while checking token secret", e);
        }

        if (userWithSecret == null) {
            return null;
        }

        return userWithSecret;
    }

    public static class ProjectSpec {
        public long projectId;
        public String projectName;

    }

    private AuthenticatedUser toAuthenticationV2(DomainData domain, ProjectSpec projectSpec,
            UserWithSecret userWithSecret) throws CloudException {
        ProjectData project = null;
        ProjectRoles projectRoles = null;

        UserData user = userWithSecret.getUserData();

        if (projectSpec.projectId != 0) {
            return buildProjectToken(domain, projectSpec.projectId, userWithSecret);
        } else if (!Strings.isNullOrEmpty(projectSpec.projectName)) {
            for (ProjectRoles i : user.getProjectRolesList()) {
                ProjectData p = authRepository.getProjects().find(i.getProject());
                if (p == null) {
                    continue;
                }

                if (projectSpec.projectName.equals(p.getName())) {
                    projectRoles = i;
                    project = p;
                    break;
                }
            }

            if (projectRoles == null) {
                return null;
            }

            return buildProjectToken(domain, project, userWithSecret);
        }

        // else if (user.hasDefaultProjectId()) {
        // long projectId = user.getDefaultProjectId();
        //
        // projectRoles = Users.findProjectRoles(user, projectId);
        //
        // if (projectRoles != null) {
        // project = authRepository.getProjects().find(projectId);
        // if (project == null) {
        // log.warn("Cannot find project {}", projectId);
        // projectRoles = null;
        // }
        // }
        //
        // if (projectRoles == null) {
        // // Not an error
        // log.info("User {} does not have access to their default project",
        // user.getId());
        // } else {
        // scope = TokenScope.Project;
        // }
        // }

        assert (project == null);

        // For V2, we treat the scope as a domain if it is unspecified
        return buildDomainToken(domain, userWithSecret);
    }

    protected UserWithSecret authenticate(DomainData domain, String username, String password) throws CloudException {
        if (Strings.isNullOrEmpty(username)) {
            return null;
        }

        if (Strings.isNullOrEmpty(password)) {
            return null;
        }

        CredentialData credential = authRepository.getUsernames(domain).find(username);
        if (credential == null) {
            return null;
        }

        UserData user = authRepository.getUsers().find(credential.getUserId());
        if (user == null) {
            return null;
        }

        UserWithSecret userWithSecret = secretService.checkPassword(user, credential, password);
        if (userWithSecret == null) {
            // TODO: Throttle?
            log.debug("Password mismatch for {}", username);
            return null;
        }

        return userWithSecret;
    }

    protected TokenInfo findTokenInfo(String tokenId) {
        try {
            TokenInfo tokenInfo = tokenService.findValidToken(tokenId);
            return tokenInfo;
        } catch (Exception e) {
            log.warn("Unexpected error while reading token", e);
            return null;
        }
    }

    // protected User findUserFromToken(TokenInfo tokenInfo) throws
    // CloudException {
    // if (tokenInfo == null) {
    // return null;
    // }
    //
    // if (TokenAuth.hasExpired(tokenInfo)) {
    // // This is treated the same as an invalid token
    // return null;
    // }
    //
    // long userId = tokenInfo.getUserId();
    // return new User(userId);
    // // authStore.getUsers().find(userId);
    // }

    protected DomainData findDomainFromToken(TokenInfo tokenInfo) throws CloudException {
        if (tokenInfo == null) {
            return null;
        }

        long domainId = -1;
        switch (tokenInfo.getTokenScope()) {
        case Domain:
            domainId = tokenInfo.getDomainId();
            break;

        case Project:
            if (tokenInfo.hasDomainId()) {
                domainId = tokenInfo.getDomainId();
            } else {
                // boolean resolveFromProject = true;
                // if (resolveFromProject) {
                // log.warn("Resolving domain from project for token");
                //
                // long projectId = tokenInfo.getProjectId();
                // ProjectData project =
                // authRepository.getProjects().find(projectId);
                // if (project != null) {
                // domainId = project.getDomainId();
                // }
                // }
            }

            if (domainId == -1 || domainId == 0) {
                throw new UnsupportedOperationException("No domain set in project-scoped token");
            }
            break;

        default:
            break;
        }

        if (domainId >= 0) {
            return authRepository.getDomains().find(domainId);
        } else {
            return null;
        }
    }

    @Override
    public TokenInfo buildTokenInfo(AuthenticatedUser authentication) {
        TokenScope tokenScope = authentication.getScope();

        Date expiration = TOKEN_VALIDITY.addTo(new Date());

        TokenInfo.Builder token = TokenInfo.newBuilder();
        token.setUserId(authentication.getUserId());
        // Millisecond resolution is overkill
        // (and size matters, because this token gets passed as a cookie))
        token.setExpiration(expiration.getTime() / 1000L);

        token.setTokenScope(tokenScope);

        ProjectData project = authentication.getProject();
        if (project != null) {
            token.setProjectId(project.getId());
            if (authentication.getProjectRoleIds() != null) {
                for (long projectRoleId : authentication.getProjectRoleIds()) {
                    token.addRoles(projectRoleId);
                }
            }
        }

        token.setDomainId(authentication.getDomainId());
        for (long domainRoleId : authentication.getDomainRoleIds(authentication.getDomainId())) {
            token.addDomainRoles(domainRoleId);
        }

        {
            ByteString tokenSecret = secretService.buildTokenSecret(authentication);
            token.setTokenSecret(tokenSecret);
        }
        return token.build();

    }

    @Override
    @Transactional
    public AuthenticatedUser authenticate(Long projectId, String username, String password) throws CloudException {
        DomainData domain = identityService.getDefaultDomain();
        UserWithSecret userWithSecret = authenticate(domain, username, password);
        if (userWithSecret == null) {
            return null;
        }

        if (projectId == null) {
            return buildDomainToken(domain, userWithSecret);
        } else {
            return buildProjectToken(domain, projectId, userWithSecret);
        }
    }

    @Override
    @Transactional
    public AuthenticatedUser authenticate(Long projectId, ClientCertificate clientCertificate, ByteString challenge,
            ByteString response) throws CloudException {
        DomainData domain = identityService.getDefaultDomain();
        UserWithSecret userWithSecret = authenticate(domain, clientCertificate, challenge, response);
        if (userWithSecret == null) {
            return null;
        }

        if (projectId == null) {
            return buildDomainToken(domain, userWithSecret);
        } else {
            return buildProjectToken(domain, projectId, userWithSecret);
        }
    }

    @Override
    @Transactional
    public ByteString getChallenge(ClientCertificate clientCertificate) throws CloudException {
        DomainData domain = identityService.getDefaultDomain();

        String keyId = toCredentialKey(clientCertificate.getPublicKeySha1());
        CredentialData credential = authRepository.getPublicKeyCredentials(domain.getId()).find(keyId);
        if (credential == null) {
            log.info("No credential found for {}", keyId);
            return null;
        }

        UserData user = authRepository.getUsers().find(credential.getUserId());
        if (user == null) {
            log.warn("User not found for credential {}", credential);
            return null;
        }

        return secretService.buildAuthChallenge(user, credential, clientCertificate);
    }

    private UserWithSecret authenticate(DomainData domain, ClientCertificate clientCertificate, ByteString challenge,
            ByteString response) throws CloudException {
        String keyId = toCredentialKey(clientCertificate.getPublicKeySha1());
        CredentialData credential = authRepository.getPublicKeyCredentials(domain.getId()).find(keyId);
        if (credential == null) {
            return null;
        }

        UserData user = authRepository.getUsers().find(credential.getUserId());
        if (user == null) {
            return null;
        }

        UserWithSecret userWithSecret = secretService.checkPublicKey(user, credential, clientCertificate, challenge,
                response);
        if (userWithSecret == null) {
            // TODO: Throttle?
            log.debug("Key mismatch for {}", user);
            return null;
        }

        return userWithSecret;
    }

    public static String toCredentialKey(ByteString publicKeySha1) {
        return BaseEncoding.base16().encode(publicKeySha1.toByteArray());
    }

    private AuthenticatedUser buildDomainToken(DomainData domain, UserWithSecret userWithSecret) throws CloudException {
        TokenScope scope = null;
        ProjectData project = null;
        ProjectRoles projectRoles = null;

        scope = TokenScope.Domain;

        return new AuthenticatedUser(scope, userWithSecret, project, projectRoles, domain);
    }

    private AuthenticatedUser buildProjectToken(DomainData domain, ProjectData project, UserWithSecret userWithSecret)
            throws CloudException {
        if (project == null) {
            throw new IllegalStateException();
        }

        TokenScope scope = TokenScope.Project;
        ProjectRoles projectRoles = null;

        UserData user = userWithSecret.getUserData();

        projectRoles = Users.findProjectRoles(user, project.getId());
        if (projectRoles == null) {
            return null;
        }

        return new AuthenticatedUser(scope, userWithSecret, project, projectRoles, domain);
    }

    private AuthenticatedUser buildProjectToken(DomainData domain, long projectId, UserWithSecret userWithSecret)
            throws CloudException {
        UserData user = userWithSecret.getUserData();

        ProjectRoles projectRoles = Users.findProjectRoles(user, projectId);
        if (projectRoles == null) {
            return null;
        }

        ProjectData project = authRepository.getProjects().find(projectRoles.getProject());
        if (project == null) {
            log.warn("Cannot find project {}", projectRoles.getProject());
            return null;
        }

        TokenScope scope = TokenScope.Project;
        return new AuthenticatedUser(scope, userWithSecret, project, projectRoles, domain);
    }

    @Override
    public ByteString createRegistrationChallenge(ClientCertificate clientCertificate) throws CloudException {
        AesKey secretKey = KeyczarUtils.generateSymmetricKey();
        byte[] payload = KeyczarUtils.pack(secretKey);
        byte[] plaintext = ChallengeResponses.addHeader(payload);

        // We can't encrypt because http proxies don't pass the public key :-(
        // It shouldn't add anything to security anyway
        // byte[] ciphertext = ChallengeResponses.encrypt(publicKey, plaintext);
        byte[] ciphertext = plaintext;

        ciphertext = ChallengeResponses.addHeader(ciphertext);

        return ByteString.copyFrom(ciphertext);
    }

}
