package io.fathom.cloud.compute.api.os.resources;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.compute.api.os.model.Services;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.inject.persist.Transactional;

@Path("/openstack/compute/{project}/os-services")
@Transactional
public class OsServicesResource extends ComputeResourceBase {
    private static final Logger log = LoggerFactory.getLogger(OsServicesResource.class);

    @GET
    @Produces({ JSON })
    public Services listServices() throws CloudException {
        warnStub();

        Services services = new Services();
        services.services = Lists.newArrayList();
        return services;
    }

}
