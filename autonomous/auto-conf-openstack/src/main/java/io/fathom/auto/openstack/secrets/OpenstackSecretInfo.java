package io.fathom.auto.openstack.secrets;

import io.fathom.auto.config.SecretKeys.SecretInfo;
import io.fathom.cloud.openstack.client.RestClientException;
import io.fathom.cloud.openstack.client.keystore.Secret;

import java.io.IOException;

public class OpenstackSecretInfo implements SecretInfo {

    private final Secret data;
    private final OpenstackSecretKeys parent;

    public OpenstackSecretInfo(OpenstackSecretKeys parent, Secret data) {
        this.parent = parent;
        this.data = data;
    }

    @Override
    public String getId() {
        return data.id;
    }

    @Override
    public String read() throws IOException {
        try {
            return parent.readSecret(data);
        } catch (RestClientException e) {
            throw new IOException("Error reading secret", e);
        }
    }

}
