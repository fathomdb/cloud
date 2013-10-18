package io.fathom.cloud.openstack.client.dns.model;

import java.util.Date;

public class Zone {
    public String id;
    public String pool_id;
    public String project_id;

    public String name;
    public String email;
    public Long ttl;

    public Long serial;

    public String status;
    public Long version;

    public Date created_at;
    public Date updated_at;

    // public List<Link> links;
}
