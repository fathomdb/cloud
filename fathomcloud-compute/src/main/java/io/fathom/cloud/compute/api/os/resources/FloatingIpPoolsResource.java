package io.fathom.cloud.compute.api.os.resources;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.compute.api.os.model.FloatingIpPool;
import io.fathom.cloud.compute.api.os.model.FloatingIpPools;
import io.fathom.cloud.compute.services.IpPools;
import io.fathom.cloud.protobuf.CloudModel.VirtualIpPoolData;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import com.google.common.collect.Lists;

@Path("/openstack/compute/{project}/os-floating-ip-pools")
public class FloatingIpPoolsResource extends ComputeResourceBase {

    @Inject
    IpPools ipPools;

    @GET
    @Produces({ JSON })
    public FloatingIpPools list() throws CloudException {
        FloatingIpPools response = new FloatingIpPools();
        response.floatingIpPools = Lists.newArrayList();

        List<VirtualIpPoolData> pools = ipPools.listVirtualIpPools(getProject());
        for (VirtualIpPoolData pool : pools) {
            FloatingIpPool floatingIpPool = new FloatingIpPool();

            String name = FloatingIpsResource.getLabel(pool);

            floatingIpPool.name = name;
            response.floatingIpPools.add(floatingIpPool);
        }

        return response;
    }

}
