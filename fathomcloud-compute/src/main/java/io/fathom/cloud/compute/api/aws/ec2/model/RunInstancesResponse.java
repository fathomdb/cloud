package io.fathom.cloud.compute.api.aws.ec2.model;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "RunInstancesResponse", namespace = "http://ec2.amazonaws.com/doc/2012-12-01/")
public class RunInstancesResponse {
    public String requestId;

    public String reservationId;

    public String ownerId;

    @XmlElementWrapper(name = "instancesSet")
    @XmlElement(name = "item")
    public List<Instance> instances;

    @XmlElementWrapper(name = "groupSet")
    @XmlElement(name = "item")
    public List<Group> groups;

}
