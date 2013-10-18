package io.fathom.cloud.openstack.client.identity;

import io.fathom.cloud.openstack.client.RestClientException;
import io.fathom.http.HttpClient;

public interface TokenProvider {

    HttpClient getHttpClient();

    void reset();

    String getToken() throws RestClientException;

    String findEndpoint(String serviceType) throws RestClientException;
}
