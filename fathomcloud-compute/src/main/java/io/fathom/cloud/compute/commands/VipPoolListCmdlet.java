package io.fathom.cloud.compute.commands;

import io.fathom.cloud.compute.services.IpPools;
import io.fathom.cloud.protobuf.CloudModel.VirtualIpPoolData;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VipPoolListCmdlet extends ListCmdlet {
    private static final Logger log = LoggerFactory.getLogger(VipPoolListCmdlet.class);

    @Inject
    IpPools ipPools;

    public VipPoolListCmdlet() {
        super("vip-pool-list");
    }

    @Override
    protected List<VirtualIpPoolData> run0() throws Exception {
        List<VirtualIpPoolData> pools = ipPools.listVirtualIpPools(null);

        return pools;
    }

}
