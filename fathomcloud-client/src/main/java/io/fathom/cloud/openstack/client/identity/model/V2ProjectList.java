package io.fathom.cloud.openstack.client.identity.model;

import java.util.List;

public class V2ProjectList {
    // @XmlElement(name = "tenant_links")
    // public List<String> tenantLinks;

    public List<V2Project> tenants;
}
