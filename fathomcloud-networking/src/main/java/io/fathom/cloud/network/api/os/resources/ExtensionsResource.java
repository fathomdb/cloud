package io.fathom.cloud.network.api.os.resources;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.network.NetworkService;
import io.fathom.cloud.network.api.os.models.ExtensionList;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

@Path("/openstack/network/{project}/v2.0/extensions")
public class ExtensionsResource extends NetworkResourceBase {
    private static final Logger log = LoggerFactory.getLogger(ExtensionsResource.class);

    @Inject
    NetworkService networkService;

    @GET
    public ExtensionList list() throws CloudException {
        // List<SubnetData> subnets = networkService.listSubnets(getAuth());

        warnStub();

        ExtensionList model = new ExtensionList();
        model.extensions = Lists.newArrayList();
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
