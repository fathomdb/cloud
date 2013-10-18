package io.fathom.cloud.network.api.os.resources;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.network.NetworkService;
import io.fathom.cloud.network.api.os.models.PortList;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

@Path("/openstack/network/{project}/v2.0/ports")
public class PortsResource extends NetworkResourceBase {
    private static final Logger log = LoggerFactory.getLogger(PortsResource.class);

    @Inject
    NetworkService networkService;

    @GET
    public PortList list() throws CloudException {
        // List<SubnetData> subnets = networkService.listSubnets(getAuth());

        warnStub();

        PortList model = new PortList();
        model.ports = Lists.newArrayList();
        // for (SubnetData subnet : subnets) {
        // }
        return model;
    }

    // private Subnet toModel(SubnetData data) {
    // Subnet model = new Subnet();
    // model.id = Long.toString(data.getId());
    // model.name = data.getName();
    // model.enableDhcp = false;
    // model.ipVersion = 4;
    // model.networkId = Long.toString(data.getNetwork());
    // return model;
    // }

}
