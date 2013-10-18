package io.fathom.auto.openstack.metadata;

import io.fathom.cloud.openstack.client.RestClientException;
import io.fathom.cloud.openstack.client.SimpleRestClient;
import io.fathom.http.HttpClient;
import io.fathom.http.HttpRequest;
import io.fathom.http.jre.JreHttpClient;

import java.net.URI;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class MetadataClient extends SimpleRestClient {
    public static final MetadataClient INSTANCE;

    static {
        URI metadataEndpoint = URI.create("http://[fd00::feed]:8775/");

        HttpClient httpClient = JreHttpClient.create();
        MetadataClient metadataClient = new MetadataClient(httpClient, metadataEndpoint);

        INSTANCE = metadataClient;
    }

    public MetadataClient(HttpClient httpClient, URI baseUri) {
        super(httpClient, baseUri);
    }

    public Metadata getMetadata() throws RestClientException {
        HttpRequest request = buildGet("openstack/latest/meta_data.json");

        String json = doStringRequest(request);

        JsonObject metadata = (JsonObject) new JsonParser().parse(json);

        return new Metadata(metadata);
    }

    public String getSecretString(String key) throws RestClientException {
        HttpRequest request = buildGet("openstack/latest/secret/" + key);

        return doStringRequest(request);
    }
}
