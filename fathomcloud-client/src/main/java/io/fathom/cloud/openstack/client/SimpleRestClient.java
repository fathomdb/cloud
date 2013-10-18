package io.fathom.cloud.openstack.client;

import io.fathom.http.HttpClient;
import io.fathom.http.HttpMethod;
import io.fathom.http.HttpRequest;
import io.fathom.http.HttpResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.io.ByteSource;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import com.google.common.net.HttpHeaders;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

public abstract class SimpleRestClient {
    private static final Logger log = LoggerFactory.getLogger(SimpleRestClient.class);

    public static final Charset UTF_8 = Charset.forName("UTF-8");

    // public static final DefaultHttpClient DEFAULT_HTTP_CLIENT =
    // buildDefaultHttpClient();
    //
    // static DefaultHttpClient buildDefaultHttpClient() {
    // PoolingClientConnectionManager ccm = new
    // PoolingClientConnectionManager();
    // DefaultHttpClient httpClient = new DefaultHttpClient(ccm);
    //
    // return httpClient;
    // }

    final HttpClient httpConfiguration;

    protected static final Gson gson = buildGson();

    final URI baseUri;

    public SimpleRestClient(HttpClient httpConfiguration, URI baseUri) {
        this.httpConfiguration = httpConfiguration;
        this.baseUri = baseUri;
    }

    private static Gson buildGson() {
        Gson gson = new GsonBuilder().registerTypeAdapter(Date.class, new DateTypeAdapter()).create();
        return gson;
    }

    // private static ObjectMapper buildObjectMapper() {
    // ObjectMapper objectMapper = new ObjectMapper();
    // objectMapper.setSerializationInclusion(Include.NON_NULL);
    //
    // // Use JAXB annotations
    // AnnotationIntrospector introspector = new JaxbAnnotationIntrospector();
    // objectMapper = objectMapper.setAnnotationIntrospector(introspector);
    //
    // objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
    // false);
    //
    // // mapper = mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES,
    // // true);
    // // mapper =
    // // mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES,
    // // true);
    // // mapper = mapper.configure(JsonParser.Feature.ALLOW_COMMENTS,
    // // true);
    //
    // objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
    // true);
    //
    // return objectMapper;
    // }

    protected <T> T doSimpleGet(String relativePath, Class<T> clazz) throws RestClientException {
        String json = doGet(relativePath);

        return readSingleValue(clazz, json);
    }

    protected <T> T doRequest(HttpRequest request, Class<T> clazz) throws RestClientException {
        String json = doStringRequest(request);

        return readSingleValue(clazz, json);
    }

    protected <T> List<T> doListRequest(HttpRequest request, Class<T> clazz) throws RestClientException {
        String json = doStringRequest(request);

        return readList(json, clazz);
    }

    protected <T> T doPost(String relativePath, Object post, Class<T> clazz) throws RestClientException {
        String json = doPost(relativePath, post);

        return readSingleValue(clazz, json);
    }

    protected <T> T doJsonRequest(HttpRequest request, Class<T> clazz) throws RestClientException {
        String json = doStringRequest(request);

        return readSingleValue(clazz, json);
    }

    protected <T> T readSingleValue(Class<T> clazz, String json) throws RestClientException {
        if (json == null) {
            return null;
        }

        // ObjectReader reader = objectMapper.reader(clazz);
        // T ret;
        // try {
        // ret = reader.readValue(json);
        // } catch (IOException e) {
        // log.info("Error parsing JSON: " + json);
        // throw new RestClientException("Error deserializing response", e);
        // }
        //
        // return ret;

        try {
            return gson.fromJson(json, clazz);
        } catch (JsonParseException e) {
            throw new RestClientException("Error deserializing response", e);
        }
    }

    private <T> List<T> doListGet(String relativePath, Class<T> clazz) throws RestClientException {
        String json = doGet(relativePath);

        List<T> ret = readList(json, clazz);

        // ObjectReader reader = objectMapper.reader(clazz);
        // List<T> ret = Lists.newArrayList();
        // try {
        // MappingIterator<T> iterator = reader.readValues(json);
        // while (iterator.hasNext()) {
        // T next = iterator.next();
        // ret.add(next);
        // }
        // } catch (IOException e) {
        // throw new RestClientException("Error deserializing response", e);
        // }

        return ret;
    }

    private <T> List<T> readList(String json, Class<T> clazz) throws RestClientException {
        List<T> ret = new ArrayList<T>();
        try {
            JsonParser parser = new JsonParser();
            JsonArray array = parser.parse(json).getAsJsonArray();
            for (int i = 0; i < array.size(); i++) {
                T next = gson.fromJson(array.get(i), clazz);
                ret.add(next);
            }
        } catch (JsonParseException e) {
            throw new RestClientException("Error deserializing response", e);
        }
        return ret;
    }

    private String doGet(String relativePath) throws RestClientException {
        HttpRequest request = buildGet(relativePath);
        return doStringRequest(request);
    }

    protected HttpRequest buildGet(String relativePath) throws RestClientException {
        URI uri = resolve(relativePath);

        HttpRequest request = httpConfiguration.buildRequest(HttpMethod.GET, uri);
        addHeaders(request);
        return request;
    }

    protected URI resolve(String relativePath) {
        return baseUri.resolve(relativePath);
    }

    protected HttpRequest buildHead(String relativePath) throws RestClientException {
        URI uri = resolve(relativePath);

        HttpRequest request = httpConfiguration.buildRequest(HttpMethod.HEAD, uri);
        addHeaders(request);
        return request;
    }

    protected HttpRequest buildPut(String relativePath) throws RestClientException {
        URI uri = resolve(relativePath);

        HttpRequest request = httpConfiguration.buildRequest(HttpMethod.PUT, uri);
        addHeaders(request);
        return request;
    }

    protected HttpRequest buildDelete(String relativePath) throws RestClientException {
        URI uri = resolve(relativePath);

        HttpRequest request = httpConfiguration.buildRequest(HttpMethod.DELETE, uri);
        addHeaders(request);
        return request;
    }

    protected void addHeaders(HttpRequest request) throws RestClientException {

    }

    protected HttpRequest buildPost(String relativePath) throws RestClientException {
        URI uri = resolve(relativePath);

        HttpRequest request = httpConfiguration.buildRequest(HttpMethod.POST, uri);
        addHeaders(request);
        return request;
    }

    protected String doPost(String relativePath, Object data) throws RestClientException {
        HttpRequest request = buildPost(relativePath);
        setEntityJson(request, data);
        return doStringRequest(request);
    }

    protected void setEntityJson(HttpRequest request, Object data) throws RestClientException {
        String json = asJson(data);

        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        setRequestContent(request, ByteSource.wrap(json.getBytes(Charsets.UTF_8)));
    }

    protected String doStringRequest(HttpRequest request) throws RestClientException {
        HttpResponse response = executeRawRequest(request);

        try {
            String json = null;
            try (InputStream is = response.getInputStream()) {
                if (is != null) {
                    try (InputStreamReader reader = new InputStreamReader(is, Charsets.UTF_8)) {
                        json = CharStreams.toString(reader);
                    }
                }
            } catch (IOException e) {
                throw new RestClientException("Error reading response", e);
            }

            return json;
        } finally {
            closeQuietly(response);
        }
    }

    protected byte[] doByteArrayRequest(HttpRequest request) throws RestClientException {
        HttpResponse response = executeRawRequest(request);

        try {
            byte[] data = null;
            try (InputStream is = response.getInputStream()) {
                if (is != null) {
                    data = ByteStreams.toByteArray(is);
                }
            } catch (IOException e) {
                throw new RestClientException("Error reading response", e);
            }

            return data;
        } finally {
            closeQuietly(response);
        }
    }

    public static void closeQuietly(HttpResponse response) {
        if (response == null) {
            return;
        }
        try {
            response.close();
        } catch (IOException e) {
            log.warn("Error closing response", e);
        }
    }

    /**
     * Deprecated because you have to close HttpResponse. Use doByteArrayRequest
     * or doStringRequest instead??
     */
    @Deprecated
    protected HttpResponse executeRawRequest(HttpRequest request) throws RestClientException {
        int attempt = 0;
        while (true) {
            attempt++;
            log.debug("Doing HTTP {} {}", request.getMethod(), request.getUrl());

            HttpResponse response;
            int statusCode;

            try {
                response = request.doRequest();
                statusCode = response.getHttpResponseCode();
            } catch (IOException e) {
                throw new RestClientException("Error during call", e);
            }

            if (!isSuccess(request, statusCode)) {
                closeQuietly(response);

                if (shouldRetry(attempt, statusCode)) {
                    continue;
                } else {
                    throw new RestClientException("Error during API call.  StatusCode: " + statusCode, statusCode);
                }
            }

            return response;
        }
    }

    protected boolean isSuccess(HttpRequest request, int statusCode) {
        switch (statusCode) {
        case 200: // OK
        case 201: // Created
        case 202: // Accepted
        case 204:// No content
            return true;

        case 206: // Partial content
            // TODO: Check that we passed a range header?
            return true;

        default:
            return false;
        }
    }

    protected boolean shouldRetry(int attempt, int statusCode) {
        return false;
    }

    public static String asJson(Object o) {
        // ObjectWriter writer = objectMapper.writer();
        // String postDataJson;
        // try {
        // postDataJson = writer.writeValueAsString(o);
        // } catch (JsonProcessingException e) {
        // throw new IllegalStateException("Error serializing value", e);
        // }
        // return postDataJson;

        try {
            return gson.toJson(o);
        } catch (JsonParseException e) {
            throw new IllegalStateException("Error serializing value", e);
        }
    }

    protected static String urlEscape(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException();
        }
    }

    protected void setRequestContent(HttpRequest request, ByteSource entity) throws RestClientException {
        try {
            request.setRequestContent(entity);
        } catch (IOException e) {
            throw new RestClientException("Error setting request content", e);
        }
    }

    public HttpClient getHttpClient() {
        return this.httpConfiguration;
    }

    public URI getBaseUri() {
        return baseUri;
    }

}
