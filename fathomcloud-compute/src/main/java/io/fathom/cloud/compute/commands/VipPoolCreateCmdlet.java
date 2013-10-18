package io.fathom.cloud.compute.commands;

import io.fathom.cloud.compute.networks.IpRange;
import io.fathom.cloud.compute.services.IpPools;
import io.fathom.cloud.protobuf.CloudModel.VirtualIpPoolData;
import io.fathom.cloud.protobuf.CloudModel.VirtualIpPoolType;

import javax.inject.Inject;

import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VipPoolCreateCmdlet extends NetworkMapCmdlet {
    private static final Logger log = LoggerFactory.getLogger(VipPoolCreateCmdlet.class);

    @Option(name = "-label", usage = "label", required = false)
    public String label;

    @Option(name = "-cidr", usage = "cidr")
    public String cidr;

    @Option(name = "-type", usage = "type")
    public String type = "ethernet";

    @Inject
    IpPools ipPools;

    public VipPoolCreateCmdlet() {
        super("vip-pool-create");
    }

    @Override
    protected VirtualIpPoolData run0() throws Exception {
        VirtualIpPoolData.Builder b = VirtualIpPoolData.newBuilder();
        if (label != null) {
            b.setLabel(label);
        }

        type = type.toLowerCase().trim();
        if (type.equals("ethernet")) {
            b.setType(VirtualIpPoolType.LAYER_3);
        } else if (type.equals("aws")) {
            b.setType(VirtualIpPoolType.AMAZON_EC2);
        } else {
            throw new IllegalArgumentException("Unknown type (valid types are 'ethernet', 'aws')");
        }

        if (b.getType() == VirtualIpPoolType.LAYER_3) {
            if (cidr == null) {
                throw new IllegalArgumentException("CIDR is required for Ethernet-based virtual-IP pools");
            }

            IpRange range = IpRange.parse(cidr);
            // if (!range.isIpv4()) {
            // throw new IllegalArgumentException("Only IPV4 is supported");
            // }
            b.setCidr(cidr);
        }
        if (b.getType() == VirtualIpPoolType.AMAZON_EC2) {
            if (cidr != null) {
                throw new IllegalArgumentException("CIDR cannot be provided for AWS virtual-IP pools");
            }
        }

        VirtualIpPoolData created = ipPools.createVipPool(b);

        return created;
    }

}
