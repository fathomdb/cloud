package io.fathom.cloud.compute.api.os.model;

import javax.xml.bind.annotation.XmlElement;

public class WrappedFloatingIp {
    @XmlElement(name = "floating_ip")
    public FloatingIp floatingIp;
}
