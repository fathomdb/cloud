package io.fathom.cloud.identity;

import io.fathom.cloud.identity.api.os.model.v3.BrokenClientsFilter;
import io.fathom.cloud.identity.api.os.resources.CredentialsResource;
import io.fathom.cloud.identity.api.os.resources.DomainsResource;
import io.fathom.cloud.identity.api.os.resources.GroupsResource;
import io.fathom.cloud.identity.api.os.resources.IdentityVersionsResource;
import io.fathom.cloud.identity.api.os.resources.ProjectsResource;
import io.fathom.cloud.identity.api.os.resources.RolesResource;
import io.fathom.cloud.identity.api.os.resources.TenantsResource;
import io.fathom.cloud.identity.api.os.resources.TokensResource;
import io.fathom.cloud.identity.api.os.resources.UsersResource;
import io.fathom.cloud.identity.api.os.resources.V2TokensResource;
import io.fathom.cloud.identity.api.os.resources.extensions.AttachmentsResource;
import io.fathom.cloud.identity.api.os.resources.extensions.ClientAppsResource;
import io.fathom.cloud.identity.api.os.resources.extensions.RegisterResource;
import io.fathom.cloud.identity.services.AttachmentsImpl;
import io.fathom.cloud.identity.services.IdentityService;
import io.fathom.cloud.identity.services.IdentityServiceImpl;
import io.fathom.cloud.server.auth.SharedSecretTokenService;
import io.fathom.cloud.server.auth.TokenEncoder;
import io.fathom.cloud.services.Attachments;
import io.fathom.cloud.services.AuthService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathomdb.extensions.ExtensionModuleBase;
import com.fathomdb.extensions.HttpConfiguration;

public class IdentityExtension extends ExtensionModuleBase {

    private static final Logger log = LoggerFactory.getLogger(IdentityExtension.class);

    @Override
    public void addHttpExtensions(HttpConfiguration http) {
        http.bind(V2TokensResource.class);
        http.bind(TokensResource.class);
        http.bind(TenantsResource.class);
        http.bind(ProjectsResource.class);
        http.bind(DomainsResource.class);
        http.bind(GroupsResource.class);
        http.bind(UsersResource.class);
        http.bind(RolesResource.class);
        http.bind(CredentialsResource.class);
        http.bind(IdentityVersionsResource.class);

        http.bind(AttachmentsResource.class);
        http.bind(ClientAppsResource.class);

        http.bind(RegisterResource.class);

        http.bind(BrokenClientsFilter.class);
    }

    @Override
    protected void configure() {
        bind(TokenEncoder.class).to(SharedSecretTokenService.class);
        bind(IdentityService.class).to(IdentityServiceImpl.class);
        bind(LoginService.class).to(LoginServiceImpl.class);

        // External services
        bind(AuthService.class).to(AuthServiceImpl.class);
        bind(Attachments.class).to(AttachmentsImpl.class);
    }
}
