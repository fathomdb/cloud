package io.fathom.cloud.compute.api.os.resources;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.compute.api.os.model.Extensions;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.inject.persist.Transactional;

@Path("/openstack/compute/{project}/extensions")
@Transactional
public class ExtensionsResource extends ComputeResourceBase {
    private static final Logger log = LoggerFactory.getLogger(ExtensionsResource.class);

    @GET
    @Produces({ JSON })
    public Extensions listExtensions() throws CloudException {
        Extensions response = new Extensions();
        response.extensions = Lists.newArrayList();

        return response;
    }

}
