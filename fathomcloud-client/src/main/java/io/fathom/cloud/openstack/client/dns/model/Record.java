package io.fathom.cloud.openstack.client.dns.model;

import com.google.common.base.Objects;

public class Record {
    public String value;

    public Integer weight;
    public Integer priority;

    public Integer port;

    public boolean matches(Record other) {
        return matches(this, other);
    }

    private static boolean matches(Record a, Record b) {
        if (!Objects.equal(a.value, b.value)) {
            return false;
        }
        if (!Objects.equal(a.port, b.port)) {
            return false;
        }
        if (!Objects.equal(a.priority, b.priority)) {
            return false;
        }
        if (!Objects.equal(a.weight, b.weight)) {
            return false;
        }
        return true;
    }
}
