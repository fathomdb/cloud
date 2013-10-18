package io.fathom.cloud.openstack.client.keystore;

import io.fathom.cloud.openstack.client.SimpleRestClient;

import java.util.Date;
import java.util.Map;

public class Secret {
    public static final String CERTIFICATE = "certificate";
    public static final String PRIVATE_KEY = "privatekey";

    public String id;

    public String status;
    public String name;
    public String algorithm;
    public String mode;
    public int bit_length;
    public Map<String, String> content_types;

    public Date updated;
    public Date expiration;

    public String secret_href;

    // Added by us
    public String subject;
    public long version;

    @Override
    public String toString() {
        return SimpleRestClient.asJson(this);
    }

}
