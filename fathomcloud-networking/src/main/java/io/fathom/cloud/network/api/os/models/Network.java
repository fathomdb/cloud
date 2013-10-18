package io.fathom.cloud.network.api.os.models;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;

public class Network {
    public String id;

    public String name;

    public String status;

    public List<String> subnets;

    @XmlElement(name = "admin_state_up")
    public Boolean adminStateUp;

    @XmlElement(name = "tenant_id")
    public String tenantId;

    public Boolean shared;

    @XmlElement(name = "router:external")
    public Boolean routerExternal;

}
