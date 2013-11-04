package io.fathom.cloud.compute.api.os.model;

import javax.xml.bind.annotation.XmlElement;

public class AbsoluteLimits {
    @XmlElement(name = "maxTotalRAMSize")
    public Integer maxTotalRAMSize;
    @XmlElement(name = "totalRAMUsed")
    public Integer totalRAMUsed;

    @XmlElement(name = "maxTotalInstances")
    public Integer maxTotalInstances;
    @XmlElement(name = "totalInstancesUsed")
    public Integer totalInstancesUsed;

    @XmlElement(name = "maxTotalCores")
    public Integer maxTotalCores;
    @XmlElement(name = "totalCoresUsed")
    public Integer totalCoresUsed;

    @XmlElement(name = "maxTotalKeypairs")
    public Integer maxTotalKeypairs;
    @XmlElement(name = "totalKeyPairsUsed")
    public Integer totalKeyPairsUsed;

    @XmlElement(name = "maxTotalPrivateNetworks")
    public Integer maxTotalPrivateNetworks;
    @XmlElement(name = "totalPrivateNetworksUsed")
    public Integer totalPrivateNetworksUsed;

    @XmlElement(name = "maxSecurityGroups")
    public Integer maxSecurityGroups;
    @XmlElement(name = "totalSecurityGroupsUsed")
    public Integer totalSecurityGroupsUsed;

    @XmlElement(name = "maxTotalVolumeGigabytes")
    public Integer maxTotalVolumeGigabytes;
    @XmlElement(name = "totalVolumeGigabytesUsed")
    public Integer totalVolumeGigabytesUsed;

    @XmlElement(name = "maxTotalVolumes")
    public Integer maxTotalVolumes;
    @XmlElement(name = "totalVolumesUsed")
    public Integer totalVolumesUsed;

    // "maxImageMeta": 128,
    // "maxPersonality": 5,
    // "maxPersonalitySize": 10240,
    // "maxSecurityGroupRules": 20,
    // "maxSecurityGroups": 10,
    // "maxServerMeta": 128,
    // "maxTotalCores": 20,
    // "maxTotalFloatingIps": 10,
    // "maxTotalInstances": 10,
    // "maxTotalKeypairs": 100,
    // "maxTotalRAMSize": 51200

    // "maxImageMeta": 20,
    // "maxPersonality": 6,
    // "maxPersonalitySize": 10240,
    // "maxServerMeta": 20,
    // "maxTotalCores": -1,
    // "maxTotalFloatingIps": 5,

}
