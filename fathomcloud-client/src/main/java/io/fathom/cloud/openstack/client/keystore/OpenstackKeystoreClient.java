package io.fathom.cloud.openstack.client.keystore;

import io.fathom.cloud.openstack.client.OpenstackServiceClientBase;
import io.fathom.cloud.openstack.client.RestClientException;
import io.fathom.cloud.openstack.client.identity.TokenProvider;
import io.fathom.cloud.openstack.client.storage.OpenstackStorageClient;
import io.fathom.http.HttpClient;
import io.fathom.http.HttpRequest;

import java.net.URI;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenstackKeystoreClient extends OpenstackServiceClientBase {
    public static final String SERVICE_TYPE = "keystore";

    private static final Logger log = LoggerFactory.getLogger(OpenstackStorageClient.class);

    public OpenstackKeystoreClient(HttpClient httpClient, URI uri, TokenProvider tokenProvider) {
        super(httpClient, uri, tokenProvider);
    }

    public List<Secret> listSecrets() throws RestClientException {
        HttpRequest request = buildGet("secrets");
        SecretList secrets = doRequest(request, SecretList.class);
        return secrets.secrets;
    }

    public Secret findSecret(String id) throws RestClientException {
        HttpRequest request = buildGet("secrets/" + id);
        return doRequest(request, Secret.class);
    }

    public byte[] getSecret(String id, String key) throws RestClientException {
        HttpRequest request = buildGet("secrets/" + id + "/" + key);
        return doByteArrayRequest(request);
    }

}