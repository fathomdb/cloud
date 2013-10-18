package io.fathom.cloud.identity.api.os.model;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

public class Project {
    @XmlAttribute
    public String id;

    @XmlAttribute
    public String name;

    public String description;

    public Boolean enabled;

    @XmlElement(name = "domain_id")
    public String domainId;
}
