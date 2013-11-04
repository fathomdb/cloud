package io.fathom.cloud.openstack.client;

import io.fathom.cloud.openstack.client.dns.OpenstackDnsClient;
import io.fathom.cloud.openstack.client.identity.OpenstackIdentityClient;
import io.fathom.cloud.openstack.client.identity.TokenProvider;
import io.fathom.cloud.openstack.client.keystore.OpenstackKeystoreClient;
import io.fathom.cloud.openstack.client.storage.OpenstackStorageClient;

import java.net.URI;

public class OpenstackClient {
    final TokenProvider tokenProvider;

    public OpenstackClient(TokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    OpenstackDnsClient dns;

    public OpenstackDnsClient getDns() throws RestClientException {
        if (dns == null) {
            URI uri = getEndpoint(OpenstackDnsClient.SERVICE_TYPE);
            dns = new OpenstackDnsClient(tokenProvider.getHttpClient(), uri, tokenProvider);
        }
        return dns;
    }

    OpenstackKeystoreClient keystore;

    public OpenstackKeystoreClient getKeystore() throws RestClientException {
        if (keystore == null) {
            URI uri = getEndpoint(OpenstackKeystoreClient.SERVICE_TYPE);
            keystore = new OpenstackKeystoreClient(tokenProvider.getHttpClient(), uri, tokenProvider);
        }
        return keystore;
    }

    OpenstackStorageClient storage;

    public OpenstackStorageClient getStorage() throws RestClientException {
        if (storage == null) {
            URI uri = getEndpoint(OpenstackStorageClient.SERVICE_TYPE);
            storage = new OpenstackStorageClient(tokenProvider.getHttpClient(), uri, tokenProvider);
        }
        return storage;
    }

    OpenstackIdentityClient identity;

    public OpenstackIdentityClient getIdentity() throws RestClientException {
        if (identity == null) {
            URI uri = getEndpoint(OpenstackIdentityClient.SERVICE_TYPE);

            // Remove /v2.0, if it is there
            // (we are supposed to expose it in the service catalog, but that is technically wrong)
            String uriString = uri.toString();
            if (!uriString.endsWith("/")) {
                uriString += "/";
            }
            if (uriString.endsWith("/v2.0/")) {
                uriString = uriString.substring(0, uriString.length() - 5);
            }
            uri = URI.create(uriString);

            identity = new OpenstackIdentityClient(tokenProvider.getHttpClient(), uri, tokenProvider);
        }
        return identity;
    }

    private URI getEndpoint(String serviceType) throws RestClientException {
        String endpoint = tokenProvider.findEndpoint(serviceType);
        if (endpoint == null) {
            throw new RestClientException("Cannot find service for " + serviceType);
        }

        String url = endpoint;
        if (!url.endsWith("/")) {
            url += "/";
        }
        return URI.create(url);
    }

    public static OpenstackClient build(TokenProvider tokenProvider) {
        return new OpenstackClient(tokenProvider);
    }

}
