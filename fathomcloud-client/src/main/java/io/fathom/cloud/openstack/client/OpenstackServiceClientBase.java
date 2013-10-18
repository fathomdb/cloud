package io.fathom.cloud.openstack.client;

import io.fathom.cloud.openstack.client.identity.TokenProvider;
import io.fathom.http.HttpClient;
import io.fathom.http.HttpRequest;

import java.net.URI;
import java.util.List;

import com.google.common.net.HttpHeaders;

public abstract class OpenstackServiceClientBase extends SimpleRestClient {

    private static final String HEADER_AUTH_TOKEN = "X-Auth-Token";

    protected final TokenProvider tokenProvider;

    public OpenstackServiceClientBase(HttpClient httpClient, URI uri, TokenProvider tokenProvider) {
        super(httpClient, uri);
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void addHeaders(HttpRequest request) throws RestClientException {
        super.addHeaders(request);

        List<String> headers = request.getRequestHeaders(HttpHeaders.ACCEPT);
        if (headers.isEmpty()) {
            request.setHeader(HttpHeaders.ACCEPT, "application/json");
        }

        if (tokenProvider != null) {
            String token = tokenProvider.getToken();
            if (token != null) {
                request.setHeader(HEADER_AUTH_TOKEN, token);
            }
        }
    }

}
