package io.fathom.cloud.compute.networks;

import io.fathom.cloud.protobuf.CloudModel.VirtualIpData;
import io.fathom.cloud.protobuf.CloudModel.VirtualIpPoolData;

public class VirtualIp {
    final VirtualIpPoolData poolData;
    final VirtualIpData data;

    public VirtualIp(VirtualIpPoolData poolData, VirtualIpData data) {
        super();
        this.poolData = poolData;
        this.data = data;
    }

    public VirtualIpData getData() {
        return data;
    }

    public VirtualIpPoolData getPoolData() {
        return poolData;
    }

}
