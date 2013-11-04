package io.fathom.cloud.compute.api.os.model;

import java.util.Map;

import javax.xml.bind.annotation.XmlElement;

public class AvailabilityZone {
    @XmlElement(name = "zoneName")
    public String zoneName;

    @XmlElement(name = "zoneState")
    public ZoneState zoneState;

    // "hosts": null
    public Map<String, String> hosts;
}
