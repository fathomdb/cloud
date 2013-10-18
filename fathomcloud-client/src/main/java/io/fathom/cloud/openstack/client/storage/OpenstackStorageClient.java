package io.fathom.cloud.openstack.client.storage;

import io.fathom.cloud.openstack.client.OpenstackServiceClientBase;
import io.fathom.cloud.openstack.client.RestClientException;
import io.fathom.cloud.openstack.client.identity.TokenProvider;
import io.fathom.cloud.openstack.client.storage.model.StorageListChunk;
import io.fathom.cloud.openstack.client.storage.model.StorageObjectInfo;
import io.fathom.http.HttpClient;
import io.fathom.http.HttpRequest;
import io.fathom.http.HttpResponse;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import com.google.common.net.HttpHeaders;

public class OpenstackStorageClient extends OpenstackServiceClientBase {
    public static final String SERVICE_TYPE = "object-store";

    public static abstract class GetFileOption {

        public abstract void modifyRequest(HttpRequest request);
    }

    public static class Range extends GetFileOption {

        private final Long from;
        private final Long to;

        public Range(Long from, Long to) {
            if (from == null && to == null) {
                throw new IllegalArgumentException();
            }

            this.from = from;
            this.to = to;
        }

        @Override
        public void modifyRequest(HttpRequest request) {
            // Note than -123 means the last 123 bytes in a range header, so we
            // want 0-123
            if ((from == null || from == 0) && to == null) {
                return;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("bytes=");
            if (from != null) {
                sb.append(from);
            }
            sb.append('-');
            if (to != null) {
                sb.append(to);
            }

            request.setHeader("Range", sb.toString());
        }

    }

    private static final Logger log = LoggerFactory.getLogger(OpenstackStorageClient.class);

    public OpenstackStorageClient(HttpClient httpClient, URI uri, TokenProvider tokenProvider) {
        super(httpClient, uri, tokenProvider);
    }

    public StorageObjectInfo findStorageObjectInfo(String path) throws RestClientException {
        HttpRequest request = buildHead(path);
        HttpResponse response = null;
        try {
            response = executeRawRequest(request);

            StorageObjectInfo info = new StorageObjectInfo();

            {
                String header = response.getFirstHeader(HttpHeaders.CONTENT_LENGTH);
                if (header != null) {
                    info.length = Long.valueOf(header);
                }
            }

            {
                String header = response.getFirstHeader(HttpHeaders.LAST_MODIFIED);
                if (header != null) {
                    info.lastModified = new Date(header);
                }
            }

            return info;
        } catch (RestClientException e) {
            if (e.is(404)) {
                return null;
            }
            throw e;
        } finally {
            closeQuietly(response);
        }
    }

    public void putFile(String path, File src) throws RestClientException {
        HttpRequest request = buildPut(path);

        ByteSource entity = Files.asByteSource(src);
        setRequestContent(request, entity);

        doByteArrayRequest(request);
    }

    public void appendToFile(String path, ByteSource entity) throws RestClientException {
        HttpRequest request = buildPost(path);
        setRequestContent(request, entity);

        doByteArrayRequest(request);
    }

    public void putFile(String path, ByteSource entity) throws RestClientException {
        // TODO: Support metadata
        HttpRequest request = buildPut(path);
        setRequestContent(request, entity);

        doByteArrayRequest(request);
    }

    public void delete(String path) throws RestClientException {
        HttpRequest request = buildDelete(path);
        doStringRequest(request);
    }

    public StorageListChunk listObjectsChunked(String bucket, String prefix, String delimiter, int maxListingLength,
            String priorLastKey) throws RestClientException {
        String relativeUri = bucket + "?format=json";
        if (prefix != null) {
            relativeUri += "&prefix=" + urlEscape(prefix);
        }
        if (delimiter != null) {
            relativeUri += "&delimiter=" + urlEscape(delimiter);
        }
        if (priorLastKey != null) {
            throw new UnsupportedOperationException();
        }
        if (maxListingLength != 0) {
            relativeUri += "&limit=" + maxListingLength;
        }
        HttpRequest get = buildGet(relativeUri);
        List<StorageObjectInfo> objects = doListRequest(get, StorageObjectInfo.class);

        String nextKey = null;
        if (maxListingLength != 0 && objects.size() == maxListingLength) {
            // set nextKey
            throw new UnsupportedOperationException();
        }

        List<String> subdirs = new ArrayList<String>();
        List<StorageObjectInfo> files = new ArrayList<StorageObjectInfo>();

        for (StorageObjectInfo object : objects) {
            if (object.subdir == null) {
                files.add(object);
            } else {
                subdirs.add(object.subdir);
            }
        }
        return new StorageListChunk(objects, subdirs, nextKey);
    }

    public StorageObject getObject(String path, GetFileOption... options) throws RestClientException {
        return getObject(path, Arrays.asList(options));
    }

    public StorageObject getObject(String path, List<GetFileOption> options) throws RestClientException {
        HttpRequest request = buildGet(path);

        for (GetFileOption option : options) {
            option.modifyRequest(request);
        }

        HttpResponse response = null;
        try {
            response = executeRawRequest(request);

            InputStream is;
            try {
                is = response.getInputStream();
            } catch (IOException e) {
                throw new RestClientException("Error reading response", e);
            }
            if (is == null) {
                throw new IllegalStateException();
            }

            is = new CloseHttpInputStream(is, response);

            StorageObject object = new StorageObject(is);
            response = null; // Don't close

            return object;
        } catch (RestClientException e) {
            if (e.is(404)) {
                return null;
            }
            throw e;
        } finally {
            closeQuietly(response);
        }
    }

    @Override
    protected boolean shouldRetry(int attempt, int statusCode) {
        if (attempt == 1 && statusCode == 401) {
            tokenProvider.reset();
            return true;
        }
        return false;
    }

    public List<StorageObjectInfo> listChildren(String bucket, String prefix, String delimiter)
            throws RestClientException {
        List<StorageObjectInfo> ret = new ArrayList<StorageObjectInfo>();

        int maxListingLength = 2000;

        try {
            String marker = null;
            while (true) {
                StorageListChunk chunk = listObjectsChunked(bucket, prefix, delimiter, maxListingLength, marker);
                for (StorageObjectInfo o : chunk.getObjects()) {
                    ret.add(o);
                }
                marker = chunk.getPriorLastKey();
                if (marker == null) {
                    break;
                }
            }
        } catch (RestClientException e) {
            if (ret.isEmpty() && e.is(404)) {
                return null;
            }
            throw new RestClientException("Error listing children", e);
        }
        return ret;
    }

    public void createBucket(String bucket) throws RestClientException {
        HttpRequest request = buildPost(bucket);
        doStringRequest(request);
    }

}