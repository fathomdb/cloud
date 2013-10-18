package io.fathom.cloud.openstack.client.identity.model;

import java.util.List;

public class Service {
    public String id;
    public String name;
    public String type;
    public List<Endpoint> endpoints;
}