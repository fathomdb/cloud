package io.fathom.cloud.openstack.client.identity.model;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ClientApp {
    public String id;
    public String name;
    public String secret;
}
