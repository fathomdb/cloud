package io.fathom.cloud.compute.api.os.model;

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;

import com.google.gson.internal.bind.IncludeNull;

public class Flavor {
    public String id;

    @IncludeNull
    public List<Link> links;

    public String name;

    @XmlAttribute
    public Integer ram;

    @XmlAttribute
    public Integer disk;

    @XmlAttribute
    public Integer swap;

    @XmlAttribute
    public Integer vcpus;

    @XmlAttribute(name = "OS-FLV-EXT-DATA:ephemeral")
    public Integer ephemeral;
}
