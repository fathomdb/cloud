package io.fathom.cloud.identity.api.os.model.v2;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class GetTenantsResponse {
    @XmlElement(name = "tenant_links")
    public List<String> tenantLinks;

    @XmlElement(name = "tenants")
    public List<TenantDetails> tenants;

}
