package io.fathom.cloud.compute.commands;

import io.fathom.cloud.commands.TypedCmdlet;
import io.fathom.cloud.compute.services.IpPools;
import io.fathom.cloud.protobuf.CloudModel.VirtualIpPoolData;
import io.fathom.cloud.server.model.Project;

import java.util.List;

import javax.inject.Inject;

import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VipPoolUpdateCmdlet extends TypedCmdlet {
    private static final Logger log = LoggerFactory.getLogger(VipPoolUpdateCmdlet.class);

    @Option(name = "-id", usage = "id", required = true)
    public String id;

    @Option(name = "-cidr", usage = "cidr")
    public List<String> cidr;

    @Inject
    IpPools ipPools;

    public VipPoolUpdateCmdlet() {
        super("vip-pool-update");
    }

    @Override
    protected VirtualIpPoolData run0() throws Exception {
        Project project = null;
        long poolId = Long.valueOf(id);

        VirtualIpPoolData pool = ipPools.findVirtualIpPool(project, poolId);
        if (pool == null) {
            throw new IllegalArgumentException("Cannot find pool with id: " + poolId);
        }

        VirtualIpPoolData.Builder b = VirtualIpPoolData.newBuilder(pool);

        if (cidr != null) {
            b.clearCidr();
            b.addAllCidr(cidr);
        }

        VirtualIpPoolData created = ipPools.createVipPool(b);
        return created;
    }
}
