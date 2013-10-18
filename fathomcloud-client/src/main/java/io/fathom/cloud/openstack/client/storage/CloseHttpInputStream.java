package io.fathom.cloud.openstack.client.storage;

import io.fathom.cloud.openstack.client.SimpleRestClient;
import io.fathom.http.HttpResponse;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class CloseHttpInputStream extends FilterInputStream {

    private HttpResponse response;

    public CloseHttpInputStream(InputStream inner, HttpResponse response) {
        super(inner);
        this.response = response;
    }

    @Override
    public void close() throws IOException {
        super.close();

        if (response != null) {
            SimpleRestClient.closeQuietly(response);
            response = null;
        }
    }

}
