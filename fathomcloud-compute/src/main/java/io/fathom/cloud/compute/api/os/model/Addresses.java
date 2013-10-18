package io.fathom.cloud.compute.api.os.model;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;

public class Addresses {
    @XmlElement(name = "private")
    public List<Address> privateAddresses;

    @XmlElement(name = "public")
    public List<Address> publicAddresses;
}
