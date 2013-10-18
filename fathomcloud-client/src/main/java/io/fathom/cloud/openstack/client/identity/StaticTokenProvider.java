package io.fathom.cloud.openstack.client.identity;

import io.fathom.cloud.openstack.client.RestClientException;
import io.fathom.cloud.openstack.client.identity.model.Endpoint;
import io.fathom.cloud.openstack.client.identity.model.Service;
import io.fathom.cloud.openstack.client.identity.model.Token;
import io.fathom.http.HttpClient;

public class StaticTokenProvider implements TokenProvider {
    final OpenstackIdentityClient identityClient;
    final String tokenId;
    private Token tokenInfo;

    public StaticTokenProvider(OpenstackIdentityClient identityClient, String tokenId) {
        this.identityClient = identityClient;
        this.tokenId = tokenId;
    }

    @Override
    public void reset() {
    }

    public Token getTokenInfo() throws RestClientException {
        if (this.tokenInfo == null) {
            try {
                tokenInfo = identityClient.validateToken(tokenId);
            } catch (RestClientException e) {
                throw new IllegalStateException("Error while validating token", e);
            }
        }
        return this.tokenInfo;
    }

    @Override
    public String findEndpoint(String type) throws RestClientException {
        Token token = getTokenInfo();
        if (token.serviceCatalog != null) {
            for (Service service : token.serviceCatalog) {
                if (!service.type.equals(type)) {
                    continue;
                }

                for (Endpoint endpoint : service.endpoints) {
                    return endpoint.url;
                }
            }
        }
        return null;
    }

    @Override
    public String getToken() throws RestClientException {
        return tokenId;
    }

    @Override
    public HttpClient getHttpClient() {
        return identityClient.getHttpClient();
    }

}
