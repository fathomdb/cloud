package io.fathom.cloud.compute.api.aws.ec2.model;

import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

public class Instance {
    public String instanceId;
    public String imageId;
    public InstanceState instanceState;
    public String privateDnsName;
    public String dnsName;
    // public String reason;
    public int amiLaunchIndex;

    // productCodes

    public String instanceType;
    public Date launchTime;

    public Placement placement;

    public String kernelId;

    public Monitoring monitoring;

    public String privateIpAddress;
    public String ipAddress;

    @XmlElementWrapper(name = "groupSet")
    @XmlElement(name = "item")
    public List<Group> groups;

    public static class Monitoring {
        public String state;
    }

    public static class Placement {
        public String availabilityZone;

        public String groupName;

        public String tenancy;
    }

    public static class StateReason {
        public String code;
        public String message;
    }

    public String architecture;

    public String rootDeviceType;

    // <blockDeviceMapping/>

    public String virtualizationType;

    // <clientToken/>

    public String hypervisor;
    // <networkInterfaceSet/>

    public boolean ebsOptimized;
    public StateReason stateReason;
}