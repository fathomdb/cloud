package io.fathom.cloud.image.imports;

import io.fathom.cloud.blobs.TempFile;
import io.fathom.cloud.openstack.client.RestClientException;
import io.fathom.cloud.openstack.client.SimpleRestClient;
import io.fathom.http.HttpClient;
import io.fathom.http.HttpMethod;
import io.fathom.http.HttpRequest;
import io.fathom.http.HttpResponse;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import com.google.common.io.ByteStreams;

public class ImportImageClient extends SimpleRestClient {
    public ImportImageClient(HttpClient httpClient, URI baseUri) {
        super(httpClient, baseUri);
    }

    public ImageImportMetadata getMetadata(URI uri) throws RestClientException {
        HttpRequest request = getHttpClient().buildRequest(HttpMethod.GET, uri);
        addHeaders(request);

        ImageImportMetadata metadata = doRequest(request, ImageImportMetadata.class);
        return metadata;
    }

    public TempFile downloadImage(URI metadataUri, ImageImportMetadata metadata) throws IOException,
            RestClientException {
        URI uri = metadataUri.resolve(metadata.image);

        HttpRequest request = getHttpClient().buildRequest(HttpMethod.GET, uri);
        addHeaders(request);

        HttpResponse response = executeRawRequest(request);

        try {
            try (InputStream is = response.getInputStream()) {
                if (is == null) {
                    return null;
                }

                TempFile tempFile = TempFile.create();
                try {
                    try (FileOutputStream os = new FileOutputStream(tempFile.getFile())) {
                        ByteStreams.copy(is, os);
                    }

                    TempFile ret = tempFile;
                    tempFile = null;
                    return ret;
                } finally {
                    if (tempFile != null) {
                        tempFile.close();
                    }
                }
            }
        } finally {
            closeQuietly(response);
        }
    }
}