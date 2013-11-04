package io.fathom.cloud.openstack.client.identity.model;

import io.fathom.cloud.openstack.client.SimpleRestClient;

public class V2Project {
    public String id;

    public String name;

    public String description;
    public boolean enabled;

    @Override
    public String toString() {
        return SimpleRestClient.asJson(this);
    }

}
