package io.fathom.cloud.openstack.client.dns.model;

import io.fathom.cloud.openstack.client.SimpleRestClient;

import java.util.Date;
import java.util.List;

import com.google.common.base.Objects;

public class Recordset {
    public String id;
    public String zone_id;

    /**
     * The fqdn
     */
    public String name;

    public Long weight;
    public String type;
    public Long ttl;

    public List<Record> records;

    public String status;
    public Long version;
    public Date created_at;
    public Date updated_at;

    public Date deleted_at;

    public boolean matches(Recordset other) {
        return matches(this, other);
    }

    private static boolean matches(Recordset a, Recordset b) {
        if (!Objects.equal(a.name, b.name)) {
            return false;
        }
        if (!Objects.equal(a.type, b.type)) {
            return false;
        }
        if (!Objects.equal(a.ttl, b.ttl)) {
            return false;
        }
        if (!Objects.equal(a.weight, b.weight)) {
            return false;
        }
        if ((a.deleted_at == null) != (b.deleted_at == null)) {
            return false;
        }
        int aRecordsSize = a.records != null ? a.records.size() : 0;
        int bRecordsSize = b.records != null ? b.records.size() : 0;

        if (aRecordsSize != bRecordsSize) {
            return false;
        }

        for (int i = 0; i < aRecordsSize; i++) {
            if (!a.records.get(i).matches(b.records.get(i))) {
                return false;
            }
        }
        return true;
    }

    // public List<Link> links;

    @Override
    public String toString() {
        return SimpleRestClient.asJson(this);
    }

    public boolean isDeleted() {
        return deleted_at != null;
    }

}
