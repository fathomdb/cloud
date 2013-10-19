package io.fathom.cloud.compute.api.os.model;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlElement;

public class Server {
    public String accessIPv4;
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

    public String tenant_id;

    public Date updated;

    @XmlElement(name = "user_id")
    public String userId;

    // For create
    public String flavorRef;
    public String imageRef;

    @XmlElement(name = "max_count")
    public int maxCount;

    @XmlElement(name = "min_count")
    public int minCount;

    @XmlElement(name = "security_groups")
    public List<SecurityGroup> securityGroups;

    public String adminPass;

    @XmlElement(name = "key_name")
    public String keyName;

    @XmlElement(name = "availability_zone")
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
