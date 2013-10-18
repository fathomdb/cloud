package io.fathom.cloud.compute.api.aws.ec2.model;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "TerminateInstancesResponse", namespace = "http://ec2.amazonaws.com/doc/2012-12-01/")
public class TerminateInstancesResponse {
    public String requestId;

    @XmlElementWrapper(name = "instancesSet")
    @XmlElement(name = "item")
    public List<InstanceStateChange> instances;

}
