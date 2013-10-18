package io.fathom.cloud.openstack.client.identity;

import io.fathom.cloud.openstack.client.RestClientException;
import io.fathom.cloud.openstack.client.identity.model.V2AuthRequest;
import io.fathom.cloud.openstack.client.identity.model.V2AuthResponse;
import io.fathom.cloud.openstack.client.identity.model.V2AuthRequest.PasswordCredentials;
import io.fathom.http.HttpClient;

public class AuthTokenProvider implements TokenProvider {
    final OpenstackIdentityClient identityClient;
    final V2AuthRequest request;
    private V2AuthResponse response;

    public AuthTokenProvider(OpenstackIdentityClient identityClient, V2AuthRequest request) {
        super();
        this.identityClient = identityClient;
        this.request = request;
    }

    @Override
    public void reset() {
        this.response = null;
    }

    public V2AuthResponse getResponse() throws RestClientException {
        if (this.response == null) {
            this.response = identityClient.doLogin(request);
        }
        return this.response;
    }

    @Override
    public String findEndpoint(String type) throws RestClientException {
        V2AuthResponse response = getResponse();
        if (response.access != null && response.access.serviceCatalog != null) {
            for (V2AuthResponse.Service service : response.access.serviceCatalog) {
                if (!service.type.equals(type)) {
                    continue;
                }

                for (V2AuthResponse.Endpoint endpoint : service.endpoints) {
                    return endpoint.publicURL;
                }
            }
        }
        return null;
    }

    public static AuthTokenProvider build(OpenstackIdentityClient identityClient, String tenant, String username,
            String password) {
        V2AuthRequest request = new V2AuthRequest();
        request.auth = new V2AuthRequest.V2AuthCredentials();
        request.auth.tenantName = tenant;
        request.auth.passwordCredentials = new PasswordCredentials();
        request.auth.passwordCredentials.username = username;
        request.auth.passwordCredentials.password = password;

        return new AuthTokenProvider(identityClient, request);
    }

    // public static AuthTokenProvider build(OpenstackIdentityClient
    // identityClient, Configuration config) {
    // String tenant = config.get("openstack.tenant");
    // String username = config.get("openstack.username");
    // String password = config.get("openstack.password");
    //
    // return build(identityClient, tenant, username, password);
    // }

    @Override
    public String getToken() throws RestClientException {
        V2AuthResponse response = getResponse();

        return response.access.token.id;
    }

    @Override
    public HttpClient getHttpClient() {
        return identityClient.getHttpClient();
    }

}
