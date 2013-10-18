package io.fathom.cloud.compute.api.os.model;

import io.fathom.cloud.compute.api.os.resources.Unofficial;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;

public class Host {

    @XmlElement(name = "host_name")
    public String name;

    public String service;

    public String zone;

    @Unofficial
    public List<String> addresses;

    @Unofficial
    public List<Network> networks;

    @Unofficial
    public static class Network {
        public String key;

        @XmlElement(name = "is_public")
        public boolean isPublic;

        public String gateway;

        public String cidr;
    }
}
