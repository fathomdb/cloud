package io.fathom.cloud.compute.api.os.model;

import java.util.Date;

import javax.xml.bind.annotation.XmlElement;

public class TenantUsage {
    public Date start;
    public Date stop;

    @XmlElement(name = "tenant_id")
    public String tenantId;

}
