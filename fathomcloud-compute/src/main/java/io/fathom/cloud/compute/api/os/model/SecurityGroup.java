package io.fathom.cloud.compute.api.os.model;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;

public class SecurityGroup {
    public long id;

    public String description;
    public String name;

    @XmlElement(name = "tenant_id")
    public String tenantId;

    public List<SecurityGroupRule> rules;

}
