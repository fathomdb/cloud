package io.fathom.cloud.compute.api.os.model;

import javax.xml.bind.annotation.XmlElement;

public class FloatingIp {
    // "fixed_ip": null,
    // "id": 1,
    // "instance_id": null,
    // "ip": "10.10.10.1",
    // "pool": "nova"

    @XmlElement(name = "fixed_ip")
    public String fixedIp;

    public String id;

    @XmlElement(name = "instance_id", nillable = true)
    public String instanceId;

    public String ip;

    public String pool;
}
