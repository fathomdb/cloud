package io.fathom.cloud.compute.api.os.model;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;

public class TenantUsages {
    @XmlElement(name = "tenant_usages")
    public List<TenantUsage> tenantUsages;
}
