package io.fathom.cloud.compute.api.os.model;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlElement;

public class Server {
    @XmlElement(name = "accessIPv4")
    public String accessIPv4;

    @XmlElement(name = "accessIPv6")
    public String accessIPv6;

    public Addresses addresses;

    public Date created;

    public Flavor flavor;

    public String hostId;
    public String id;

    public Image image;

    public List<Link> links;

    public Map<String, String> metadata;

    public String name;

    public Integer progress;

    public String status;

    public String tenantId;

    public Date updated;

    public String userId;

    // For create
    @XmlElement(name = "flavorRef")
    public String flavorRef;

    @XmlElement(name = "imageRef")
    public String imageRef;

    public int maxCount;

    public int minCount;

    public List<SecurityGroup> securityGroups;

    @XmlElement(name = "adminPass")
    public String adminPass;

    public String keyName;

    public String availabilityZone;

    @XmlElement(name = "OS-EXT-STS:task_state")
    public String extensionTaskState;

    @XmlElement(name = "OS-EXT-STS:power_state")
    public Integer extensionPowerState;

    @XmlElement(name = "OS-EXT-STS:vm_state")
    public String extensionVmState;

    // The latest nova client posts this:
    // "networks": [{"uuid": "00000000-0000-0000-0000-000000000000"}, {"uuid":
    // "11111111-1111-1111-1111-111111111111"}]}}'

    public List<ServerNetwork> networks;

    public static class ServerNetwork {
        public String uuid;
    }

    // Bug 1202415
    // // Not a clue what this is, but horizon pukes if it isn't there
    // public Attributes attrs = new Attributes();
    //
    // public static class Attributes {
    // public String dummy;
    // }
}
