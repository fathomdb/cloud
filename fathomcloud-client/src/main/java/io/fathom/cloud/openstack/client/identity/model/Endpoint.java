package io.fathom.cloud.openstack.client.identity.model;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class Endpoint {
    public String id;

    public String name;

    @SerializedName("interface")
    public String interfaceName;

    public String region;

    public String url;

    @SerializedName("service_id")
    public String serviceId;

    public List<Link> links;
}