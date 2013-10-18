package io.fathom.cloud.openstack.client.identity;

import io.fathom.cloud.openstack.client.RestClientException;
import io.fathom.cloud.openstack.client.identity.model.RegisterRequest;
import io.fathom.cloud.openstack.client.identity.model.RegisterResponse;
import io.fathom.cloud.openstack.client.identity.model.V2AuthRequest;
import io.fathom.cloud.openstack.client.identity.model.V2AuthResponse;
import io.fathom.http.HttpClient;

import java.net.URI;
import java.security.KeyPair;

import javax.security.auth.x500.X500Principal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathomdb.crypto.CertificateAndKey;
import com.google.common.base.Strings;

public class CertificateAuthTokenProvider implements TokenProvider {
    private static final Logger log = LoggerFactory.getLogger(CertificateAuthTokenProvider.class);

    final OpenstackIdentityClient identityClient;
    private V2AuthResponse response;
    final String project;
    final CertificateAndKey certificateAndKey;

    public CertificateAuthTokenProvider(OpenstackIdentityClient identityClient, String project,
            CertificateAndKey certificateAndKey) {
        this.identityClient = identityClient;
        this.project = project;
        this.certificateAndKey = certificateAndKey;
    }

    @Override
    public void reset() {
        this.response = null;
    }

    public V2AuthResponse getResponse() throws RestClientException {
        if (this.response == null) {
            V2AuthRequest request = new V2AuthRequest();
            request.auth = new V2AuthRequest.V2AuthCredentials();
            request.auth.tenantName = project;

            V2AuthResponse responseChallenge = identityClient.doLogin(request, certificateAndKey);

            String challenge = responseChallenge.challenge;
            V2AuthRequest.ChallengeResponse challengeResponse = ChallengeResponses.respondToAuthChallenge(
                    certificateAndKey, challenge);

            request.auth.challengeResponse = challengeResponse;

            V2AuthResponse response = identityClient.doLogin(request, certificateAndKey);

            this.response = response;
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

    public static CertificateAuthTokenProvider build(OpenstackIdentityClient identityClient, String project,
            CertificateAndKey certificateAndKey) {
        return new CertificateAuthTokenProvider(identityClient, project, certificateAndKey);
    }

    @Override
    public String getToken() throws RestClientException {
        V2AuthResponse response = getResponse();

        if (response == null || response.access == null || response.access.token == null) {
            log.warn("Unable to get auth token");
            return null;
        }

        return response.access.token.id;
    }

    @Override
    public HttpClient getHttpClient() {
        return identityClient.getHttpClient();
    }

    public static OpenstackIdentityClient tryAuth(URI uri, CertificateAndKey certificateAndKey)
            throws RestClientException {
        try {
            OpenstackIdentityClient client = OpenstackIdentityClient.build(uri);

            String project = null;
            CertificateAuthTokenProvider tokenProvider = new CertificateAuthTokenProvider(client, project,
                    certificateAndKey);

            String token = tokenProvider.getToken();
            if (token != null) {
                return client.withTokenProvider(tokenProvider);
            }
            return null;
        } catch (RestClientException e) {
            if (e.is(401)) {
                return null;
            } else {
                throw e;
            }
        }
    }

    public static OpenstackIdentityClient ensureRegistered(KeyPair keypair, URI uri, String email)
            throws RestClientException {
        CertificateAndKey certificateAndKey = createSelfSigned(keypair, email);

        OpenstackIdentityClient auth = CertificateAuthTokenProvider.tryAuth(uri, certificateAndKey);
        if (auth == null) {
            registerKey(uri, email, certificateAndKey);

            auth = CertificateAuthTokenProvider.tryAuth(uri, certificateAndKey);
            if (auth == null) {
                throw new IllegalStateException("Unable to authenticate after registration");
            }
        }

        return auth;
    }

    public static CertificateAndKey createSelfSigned(KeyPair keypair, String email) {
        X500Principal subject = new X500Principal("CN=" + email);
        CertificateAndKey certificateAndKey = ChallengeResponses.createSelfSigned(subject, keypair);
        return certificateAndKey;
    }

    private static void registerKey(URI uri, String email, CertificateAndKey certificateAndKey)
            throws RestClientException {
        OpenstackIdentityClient client = OpenstackIdentityClient.build(uri);

        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.email = email;
        RegisterResponse registerResponse = client.register(registerRequest, certificateAndKey);
        V2AuthRequest.ChallengeResponse challengeResponse = ChallengeResponses.respondToRegistrationChallenge(
                certificateAndKey, registerResponse.challenge);
        registerRequest.challengeResponse = challengeResponse;
        registerResponse = client.register(registerRequest, certificateAndKey);
        if (Strings.isNullOrEmpty(registerResponse.userId)) {
            throw new RestClientException("Unable to register (invalid response)");
        }
    }

}
