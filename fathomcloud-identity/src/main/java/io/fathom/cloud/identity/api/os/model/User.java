package io.fathom.cloud.identity.api.os.model;

import io.fathom.cloud.DebugFormatter;
import io.fathom.cloud.identity.api.os.model.v2.Role;

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

public class User {
    @XmlAttribute
    public String id;

    @XmlAttribute
    public String name;

    @XmlElement(name = "roles")
    public List<Role> roles;

    public Boolean enabled;

    public String email;

    public String description;

    // For create
    @XmlElement(name = "project_id")
    public String defaultProjectId;

    @XmlElement(name = "domain_id")
    public String domainId;

    public String password;

    @Override
    public String toString() {
        return DebugFormatter.format(this);
    }

}
