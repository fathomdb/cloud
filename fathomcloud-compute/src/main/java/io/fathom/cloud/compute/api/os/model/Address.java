package io.fathom.cloud.compute.api.os.model;

import javax.xml.bind.annotation.XmlElement;

public class Address {
    @XmlElement(name = "addr")
    public String address;

    public int version;
}
