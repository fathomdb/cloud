package io.fathom.cloud.openstack.client.loadbalance.model;

import io.fathom.cloud.openstack.client.SimpleRestClient;

public class LbaasMapping {
    public String key;

    public String host;

    public String ip;
    public Integer port;

    public String forwardUrl;

    @Override
    public String toString() {
        return SimpleRestClient.asJson(this);
    }
}
