package io.fathom.cloud.openstack.client.identity;

import io.fathom.cloud.openstack.client.OpenstackServiceClientBase;
import io.fathom.cloud.openstack.client.RestClientException;
import io.fathom.cloud.openstack.client.identity.model.ClientApp;
import io.fathom.cloud.openstack.client.identity.model.RegisterRequest;
import io.fathom.cloud.openstack.client.identity.model.RegisterResponse;
import io.fathom.cloud.openstack.client.identity.model.Token;
import io.fathom.cloud.openstack.client.identity.model.V2AuthRequest;
import io.fathom.cloud.openstack.client.identity.model.V2AuthResponse;
import io.fathom.cloud.openstack.client.identity.model.V2ProjectList;
import io.fathom.cloud.openstack.client.identity.model.V3Project;
import io.fathom.cloud.openstack.client.identity.model.WrappedV3Project;
import io.fathom.http.HttpClient;
import io.fathom.http.HttpMethod;
import io.fathom.http.HttpRequest;
import io.fathom.http.SslConfiguration;
import io.fathom.http.jre.JreHttpClient;

import java.io.IOException;
import java.net.URI;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathomdb.crypto.CertificateAndKey;
import com.fathomdb.crypto.SimpleClientCertificateKeyManager;
import com.google.common.io.ByteSource;

// If we're injected, we should be a singleton
@Singleton
public class OpenstackIdentityClient extends OpenstackServiceClientBase {
    private static final Logger log = LoggerFactory.getLogger(OpenstackIdentityClient.class);

    public OpenstackIdentityClient(HttpClient httpClient, URI uri, TokenProvider tokenProvider) {
        super(httpClient, uri, tokenProvider);
    }

    // @Inject
    // public OpenstackIdentityClient(HttpClient httpClient, Configuration
    // config) {
    // super(httpClient, URI.create(config.get("identity.url")));
    // }

    public V2AuthResponse doLogin(V2AuthRequest request) throws RestClientException {
        return doPost("v2.0/tokens", request, V2AuthResponse.class);
    }

    public V2AuthResponse doLogin(V2AuthRequest authRequest, CertificateAndKey certificateAndKey)
            throws RestClientException {
        URI uri = resolve("v2.0/tokens");

        HttpClient httpClient = getHttpClient(certificateAndKey);
        HttpRequest httpRequest = httpClient.buildRequest(HttpMethod.POST, uri);
        addHeaders(httpRequest);

        addHeaders(httpRequest, certificateAndKey);

        setEntityJson(httpRequest, authRequest);
        return doJsonRequest(httpRequest, V2AuthResponse.class);
    }

    private void addHeaders(HttpRequest httpRequest, CertificateAndKey certificateAndKey) {
        // Until haproxy can pass through the key, we'll need this hack

        // String keysig =
        // OpenSshUtils.getSignatureString(certificateAndKey.getPublicKey()).toUpperCase();
        // httpRequest.setHeader("X-SSL-Client-Key-SHA1", keysig);

        // byte[] pubkey = certificateAndKey.getPublicKey().getEncoded();
        // httpRequest.setHeader("X-SSL-Client-Key",
        // BaseEncoding.base16().encode(pubkey).toUpperCase());
    }

    public RegisterResponse register(RegisterRequest request, CertificateAndKey certificateAndKey)
            throws RestClientException {
        URI uri = resolve("extensions/register");

        HttpClient httpClient = getHttpClient(certificateAndKey);
        HttpRequest httpRequest = httpClient.buildRequest(HttpMethod.POST, uri);
        addHeaders(httpRequest);

        addHeaders(httpRequest, certificateAndKey);

        setEntityJson(httpRequest, request);
        return doJsonRequest(httpRequest, RegisterResponse.class);
    }

    private HttpClient getHttpClient(CertificateAndKey certificateAndKey) {
        SimpleClientCertificateKeyManager keyManager = new SimpleClientCertificateKeyManager(certificateAndKey);

        SslConfiguration sslConfiguration = getHttpClient().getSslConfiguration().copyWithNewKeyManager(keyManager);
        HttpClient httpClient = getHttpClient().withSsl(sslConfiguration);
        return httpClient;
    }

    public Token validateToken(String token) throws RestClientException {
        HttpRequest request = buildGet("v3/auth/tokens");

        request.setHeader("X-Auth-Token", token);
        request.setHeader("X-Subject-Token", token);

        return doRequest(request, Token.class);
    }

    public ClientApp getClientApp(ClientApp app) throws RestClientException {
        return doPost("extensions/client", app, ClientApp.class);
    }

    public byte[] getClientData(String secret, String token, String appId, String userId) throws RestClientException {
        HttpRequest request = buildGet("extensions/attachment/user/" + urlEscape(userId) + "/" + urlEscape(appId)
                + "?secret=" + urlEscape(secret));

        request.setHeader("X-Auth-Token", token);
        return doByteArrayRequest(request);
    }

    public void setClientData(String secret, String token, String appId, String userId, byte[] data)
            throws RestClientException {
        HttpRequest request = buildPut("extensions/attachment/user/" + urlEscape(userId) + "/" + urlEscape(appId)
                + "?secret=" + urlEscape(secret));

        request.setHeader("X-Auth-Token", token);
        try {
            request.setRequestContent(ByteSource.wrap(data));
        } catch (IOException e) {
            throw new RestClientException("Error setting request content", e);
        }

        doStringRequest(request);
    }

    public static OpenstackIdentityClient build(URI uri) {
        return new OpenstackIdentityClient(JreHttpClient.create(), uri, null);
    }

    public V2ProjectList listProjects() throws RestClientException {
        HttpRequest request = buildGet("v2.0/tenants");

        return doRequest(request, V2ProjectList.class);
    }

    public OpenstackIdentityClient withTokenProvider(TokenProvider tokenProvider) {
        return new OpenstackIdentityClient(getHttpClient(), getBaseUri(), tokenProvider);
    }

    public V3Project createProject(V3Project project) throws RestClientException {
        WrappedV3Project request = new WrappedV3Project();
        request.project = project;
        WrappedV3Project response = doPost("v3/projects", request, WrappedV3Project.class);
        return response.project;
    }

    public Identity getUtils() {
        return new Identity(this);
    }

}