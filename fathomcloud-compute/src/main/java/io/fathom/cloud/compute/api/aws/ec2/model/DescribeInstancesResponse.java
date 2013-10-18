package io.fathom.cloud.compute.api.aws.ec2.model;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "DescribeInstancesResponse", namespace = "http://ec2.amazonaws.com/doc/2012-12-01/")
public class DescribeInstancesResponse {
    public String requestId;

    @XmlElementWrapper(name = "reservationSet")
    @XmlElement(name = "item")
    public List<ReservationSetItem> reservations;

    public static class ReservationSetItem {
        public String reservationId;
        public String ownerId;

        @XmlElementWrapper(name = "groupSet")
        @XmlElement(name = "item")
        public List<Group> groups;

        @XmlElementWrapper(name = "instancesSet")
        @XmlElement(name = "item")
        public List<Instance> instances;
    }
}
