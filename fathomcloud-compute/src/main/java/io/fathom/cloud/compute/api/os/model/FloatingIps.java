package io.fathom.cloud.compute.api.os.model;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class FloatingIps {
    @XmlElement(name = "floating_ips")
    public List<FloatingIp> floatingIps;
}
