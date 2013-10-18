package io.fathom.cloud.compute.api.os.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class WrappedSecurityGroupRule {
    @XmlElement(name = "security_group_rule")
    public SecurityGroupRule rule;
}
