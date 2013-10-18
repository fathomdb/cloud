package io.fathom.cloud.compute.api.os.resources;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.compute.api.os.model.AbsoluteLimits;
import io.fathom.cloud.compute.api.os.model.Limits;
import io.fathom.cloud.compute.api.os.model.LimitsResponse;
import io.fathom.cloud.server.resources.FathomCloudResourceBase;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import com.google.common.collect.Lists;
import com.google.inject.persist.Transactional;

@Path("/openstack/compute/{project}/limits")
@Transactional
public class LimitsResource extends FathomCloudResourceBase {
    @GET
    @Produces({ JSON })
    public LimitsResponse doLimitsGet() throws CloudException {
        LimitsResponse response = new LimitsResponse();

        response.limits = new Limits();

        response.limits.absoluteLimits = new AbsoluteLimits();

        int instanceLimit = 1000;
        response.limits.absoluteLimits.maxTotalInstances = instanceLimit;
        response.limits.absoluteLimits.totalInstancesUsed = 0;

        response.limits.absoluteLimits.maxTotalCores = instanceLimit * 16;
        response.limits.absoluteLimits.totalCoresUsed = 0;

        response.limits.absoluteLimits.maxTotalRAMSize = instanceLimit * 32 * 1024;
        response.limits.absoluteLimits.totalRAMUsed = 0;

        response.limits.rates = Lists.newArrayList();

        return response;
    }

}
