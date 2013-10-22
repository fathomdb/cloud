package io.fathom.cloud.compute.networks;

import io.fathom.cloud.protobuf.CloudModel.VirtualIpPoolData;
import io.fathom.cloud.protobuf.CloudModel.VirtualIpPoolType;

/**
 * A pool of EC2 floating IP addresses
 * 
 */
public class Ec2IpNetworkPool extends MappableIpNetworkPool {
    Ec2IpNetworkPool(NetworkPools networkPools, VirtualIpPoolData data) {
        super(networkPools, data);

        if (data.getType() != VirtualIpPoolType.AMAZON_EC2) {
            throw new IllegalArgumentException();
        }
    }
}
