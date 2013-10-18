package io.fathom.cloud.compute.api.os.model;

import java.util.Map;

public class AvailabilityZone {
    public String zoneName;
    public ZoneState zoneState;

    // "hosts": null
    public Map<String, String> hosts;
}
