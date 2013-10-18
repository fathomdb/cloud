package io.fathom.cloud.network.api.os.models;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;

public class Subnet {
    public String id;

    @XmlElement(name = "network_id")
    public String networkId;

    public String name;

    @XmlElement(name = "ip_version")
    public int ipVersion;

    public String cidr;

    @XmlElement(name = "gateway_ip")
    public String gatewayIp;

    @XmlElement(name = "dns_nameservers")
    public List<String> dnsNameservers;

    @XmlElement(name = "enable_dhcp")
    public boolean enableDhcp;

    @XmlElement(name = "tenant_id")
    public String tenantId;

}
