package io.fathom.cloud.compute.api.os.model;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;

public class AvailabilityZoneList {
    @XmlElement(name = "availabilityZoneInfo")
    public List<AvailabilityZone> availabilityZoneInfo;
}
