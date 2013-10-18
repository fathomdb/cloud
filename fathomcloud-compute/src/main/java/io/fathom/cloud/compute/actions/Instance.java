package io.fathom.cloud.compute.actions;

import io.fathom.cloud.compute.networks.IpRange;
import io.fathom.cloud.protobuf.CloudModel.CidrData;

public class Instance {

    public static IpRange toIpRange(CidrData cidr) {
        IpRange range = IpRange.build(cidr.getAddress().toByteArray(), cidr.getPrefixLength());
        return range;
    }

}
