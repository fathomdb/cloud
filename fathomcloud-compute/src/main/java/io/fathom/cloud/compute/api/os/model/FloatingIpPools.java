package io.fathom.cloud.compute.api.os.model;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class FloatingIpPools {
    @XmlElement(name = "floating_ip_pools")
    public List<FloatingIpPool> floatingIpPools;
}
