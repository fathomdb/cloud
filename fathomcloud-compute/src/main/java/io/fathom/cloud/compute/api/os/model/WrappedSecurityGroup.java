package io.fathom.cloud.compute.api.os.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class WrappedSecurityGroup {
    @XmlElement(name = "security_group")
    public SecurityGroup securityGroup;
}
