package io.fathom.cloud.compute.commands;

import io.fathom.cloud.compute.services.IpPools;
import io.fathom.cloud.protobuf.CloudModel.VirtualIpPoolData;

import java.util.List;

import javax.inject.Inject;

import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VipPoolDeleteCmdlet extends NetworkMapCmdlet {
    private static final Logger log = LoggerFactory.getLogger(VipPoolDeleteCmdlet.class);

    @Inject
    IpPools ipPools;

    @Option(name = "-cidr", usage = "cidr", required = true)
    public String cidr;

    public VipPoolDeleteCmdlet() {
        super("vip-pool-delete");
    }

    @Override
    protected VirtualIpPoolData run0() throws Exception {
        List<VirtualIpPoolData> pools = ipPools.listVirtualIpPools(null);

        for (VirtualIpPoolData pool : pools) {
            if (!pool.hasCidr() && pool.getCidr().equals(cidr)) {
                ipPools.deleteVirtualIpPool(pool);
                return pool;
            }
        }

        throw new IllegalArgumentException();
    }

}
