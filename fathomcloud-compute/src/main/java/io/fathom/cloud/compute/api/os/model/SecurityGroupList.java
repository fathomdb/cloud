package io.fathom.cloud.compute.api.os.model;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;

public class SecurityGroupList {
    @XmlElement(name = "security_groups")
    public List<SecurityGroup> securityGroups;
}
