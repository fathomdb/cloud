package io.fathom.auto.config;

import io.fathom.auto.openstack.metadata.Metadata;
import io.fathom.auto.openstack.metadata.MetadataClient;
import io.fathom.cloud.openstack.client.OpenstackClient;
import io.fathom.cloud.openstack.client.RestClientException;
import io.fathom.cloud.openstack.client.identity.OpenstackIdentityClient;
import io.fathom.cloud.openstack.client.identity.StaticTokenProvider;
import io.fathom.cloud.openstack.client.storage.OpenstackStorageClient;
import io.fathom.http.HttpClient;

import java.net.URI;

public class OpenstackConfig {

    Metadata metadata;

    public Metadata getMetadata() throws RestClientException {
        if (metadata == null) {
            MetadataClient metadataClient = MetadataClient.INSTANCE;
            Metadata metadata = metadataClient.getMetadata();
            this.metadata = metadata;
        }
        return metadata;
    }

    public OpenstackStorageClient getStorageClient() throws RestClientException {
        return getOpenstackClient().getStorage();
    }

    OpenstackClient client;

    public OpenstackClient getOpenstackClient() throws RestClientException {
        if (client == null) {
            MetadataClient metadataClient = MetadataClient.INSTANCE;

            String serviceToken = metadataClient.getSecretString("token");
            serviceToken = serviceToken.trim();
            Metadata metadata = getMetadata();

            URI identityEndpoint = URI.create(metadata.getTopLevel("identity_uri"));

            HttpClient httpClient = metadataClient.getHttpClient();
            OpenstackIdentityClient identityClient = new OpenstackIdentityClient(httpClient, identityEndpoint, null);

            StaticTokenProvider tokenProvider = new StaticTokenProvider(identityClient, serviceToken);

            this.client = OpenstackClient.build(tokenProvider);
        }
        return client;
    }

    public String getInstanceProperty(String key) {
        if (metadata == null) {
            throw new IllegalStateException();
        }
        String value = System.getProperty(key);
        if (value == null) {
            value = metadata.findMeta(key);
        }
        return value;
    }
}
