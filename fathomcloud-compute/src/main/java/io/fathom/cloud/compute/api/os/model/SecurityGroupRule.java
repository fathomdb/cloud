package io.fathom.cloud.compute.api.os.model;

import javax.xml.bind.annotation.XmlElement;

public class SecurityGroupRule {
    public int id;

    @XmlElement(name = "parent_group_id")
    public String parentGroupId;

    @XmlElement(name = "from_port")
    public int fromPort;

    @XmlElement(name = "to_port")
    public int toPort;

    @XmlElement(name = "ip_protocol")
    public String protocol;

    // For create
    @XmlElement(name = "cidr")
    public String cidr;

    @XmlElement(name = "group_id")
    public String srcGroupId;

    // For read
    public static class IpRange {
        public String cidr;
    }

    @XmlElement(name = "ip_range")
    public IpRange ipRange;

    public static class Group {
        public String name;

        @XmlElement(name = "tenant_id")
        public String tenantId;
    }

    @XmlElement(name = "group")
    public Group group;
}
