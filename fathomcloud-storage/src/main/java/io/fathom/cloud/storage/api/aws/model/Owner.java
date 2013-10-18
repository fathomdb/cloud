package io.fathom.cloud.storage.api.aws.model;

import javax.xml.bind.annotation.XmlElement;

public class Owner {

    @XmlElement(name = "ID")
    public String id;

    @XmlElement(name = "DisplayName")
    public String displayName;

}
