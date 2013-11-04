package io.fathom.cloud.openstack.client.identity.model;

import io.fathom.cloud.openstack.client.SimpleRestClient;

import java.util.List;

public class V2ProjectList {
    // @XmlElement(name = "tenant_links")
    // public List<String> tenantLinks;

    public List<V2Project> tenants;

    @Override
    public String toString() {
        return SimpleRestClient.asJson(this);
    }

}
