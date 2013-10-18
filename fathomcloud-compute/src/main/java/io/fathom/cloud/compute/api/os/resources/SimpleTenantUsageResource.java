package io.fathom.cloud.compute.api.os.resources;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.compute.api.os.model.TenantUsage;
import io.fathom.cloud.compute.api.os.model.TenantUsages;
import io.fathom.cloud.compute.api.os.model.WrappedTenantUsage;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.inject.persist.Transactional;

@Path("/openstack/compute/{project}/os-simple-tenant-usage")
@Transactional
public class SimpleTenantUsageResource extends ComputeResourceBase {
    private static final Logger log = LoggerFactory.getLogger(SimpleTenantUsageResource.class);

    @GET
    @Path("{id}")
    @Produces({ JSON })
    public WrappedTenantUsage getTenantUsage(@PathParam("id") String id) throws CloudException {
        WrappedTenantUsage response = new WrappedTenantUsage();

        response.tenantUsage = new TenantUsage();
        return response;
    }

    @GET
    @Produces({ JSON })
    public TenantUsages getOverallUsage() throws CloudException {
        TenantUsages response = new TenantUsages();
        response.tenantUsages = Lists.newArrayList();

        return response;
    }

}
