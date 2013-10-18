package io.fathom.cloud.identity.api.os.model.v2;

import javax.xml.bind.annotation.XmlAttribute;

public class Tenant {
    @XmlAttribute
    public String id;

    @XmlAttribute
    public String name;

    @Override
    public String toString() {
        return "Tenant [id=" + id + ", name=" + name + "]";
    }

}
