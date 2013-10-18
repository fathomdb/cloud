package io.fathom.cloud.compute.api.os.model;

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;

public class Flavor {
    public String id;

    public List<Link> links;

    public String name;

    @XmlAttribute
    public int ram;

    @XmlAttribute
    public int disk;

    @XmlAttribute
    public int swap;

    @XmlAttribute
    public int vcpus;

    @XmlAttribute(name = "OS-FLV-EXT-DATA:ephemeral")
    public int ephemeral;
}
