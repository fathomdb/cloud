package io.fathom.cloud.compute.api.os.model;

import javax.xml.bind.annotation.XmlElement;

public class WrappedTenantUsage {

    @XmlElement(name = "tenant_usage")
    public TenantUsage tenantUsage;

}
