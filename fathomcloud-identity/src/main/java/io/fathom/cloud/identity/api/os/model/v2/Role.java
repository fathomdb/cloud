package io.fathom.cloud.identity.api.os.model.v2;

import javax.xml.bind.annotation.XmlAttribute;

public class Role {
    @XmlAttribute
    public String id;

    @XmlAttribute
    public String name;

    @XmlAttribute(name = "tenantId")
    public String tenantId;

    @XmlAttribute
    public String description;

    @XmlAttribute(name = "serviceId")
    public String serviceId;

}
