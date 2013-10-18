package io.fathom.cloud.identity.api.os.model.v3;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;

public class Endpoint {
    public String id;

    public String name;

    @XmlElement(name = "interface")
    public String interfaceName;

    public String region;

    public String url;

    @XmlElement(name = "service_id")
    public String serviceId;

    public List<Link> links;
}