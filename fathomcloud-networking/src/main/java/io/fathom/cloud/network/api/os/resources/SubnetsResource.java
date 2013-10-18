package io.fathom.cloud.network.api.os.resources;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.network.NetworkService;
import io.fathom.cloud.network.api.os.models.Subnet;
import io.fathom.cloud.network.api.os.models.Subnets;
import io.fathom.cloud.network.api.os.models.WrappedSubnet;
import io.fathom.cloud.protobuf.NetworkingModel.SubnetData;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

@Path("/openstack/network/{project}/v2.0/subnets")
public class SubnetsResource extends NetworkResourceBase {
    private static final Logger log = LoggerFactory.getLogger(SubnetsResource.class);

    @Inject
    NetworkService networkService;

    @GET
    @Produces({ JSON })
    public Subnets listSubnets() throws CloudException {
        List<SubnetData> subnets = networkService.listSubnets(getAuth());

        Subnets model = new Subnets();
        model.subnets = Lists.newArrayList();
        for (SubnetData subnet : subnets) {
            model.subnets.add(toModel(subnet));
        }
        return model;
    }

    @GET
    @Path("{id}")
    @Produces({ JSON })
    public WrappedSubnet getSubnet(@PathParam("id") long id) throws CloudException {
        SubnetData data = networkService.findSubnet(getAuth(), id);
        if (data == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        WrappedSubnet ret = new WrappedSubnet();
        ret.subnet = toModel(data);
        return ret;
    }

    // @PUT
    // @Path("{id}")
    // @Produces({ JSON })
    // @Consumes({ JSON })
    // public WrappedNetwork updateNetwork(@PathParam("id") long id,
    // WrappedNetwork request) throws CloudException {
    // Network network = request.network;
    // NetworkData.Builder b = toBuilder(network);
    //
    // NetworkData data = networkService.updateNetwork(getAuth(), id, b);
    // if (data == null) {
    // throw new WebApplicationException(Status.NOT_FOUND);
    // }
    //
    // WrappedNetwork ret = new WrappedNetwork();
    // ret.network = toModel(data);
    // return ret;
    // }
    //
    // @DELETE
    // @Path("{id}")
    // @Produces({ JSON })
    // public Response deleteNetwork(@PathParam("id") long id, WrappedNetwork
    // request) throws CloudException {
    // Status status = networkService.deleteNetwork(getAuth(), id);
    //
    // if (status != null) {
    // throw new WebApplicationException(status);
    // }
    //
    // ResponseBuilder response = Response.noContent();
    // return response.build();
    // }

    @POST
    @Produces({ JSON })
    public WrappedSubnet createSubnet(WrappedSubnet request) throws CloudException {
        Subnet subnet = request.subnet;

        SubnetData.Builder b = toBuilder(subnet);

        SubnetData data = networkService.createSubnet(getAuth(), b);

        WrappedSubnet ret = new WrappedSubnet();
        ret.subnet = toModel(data);
        return ret;
    }

    private SubnetData.Builder toBuilder(Subnet subnet) {
        if (subnet == null) {
            throw new IllegalArgumentException();
        }
        SubnetData.Builder b = SubnetData.newBuilder();
        if (subnet.name != null) {
            b.setName(subnet.name);
        }

        if (subnet.tenantId != null) {
            b.setProject(Long.valueOf(subnet.tenantId));
        }

        if (subnet.networkId != null) {
            b.setNetwork(Long.valueOf(subnet.networkId));
        }

        if (subnet.cidr != null) {
            b.setCidr(subnet.cidr);
        }

        if (subnet.ipVersion != 0) {
            b.setIpVersion(subnet.ipVersion);
        } else {
            b.setIpVersion(4);
        }

        return b;
    }

    private Subnet toModel(SubnetData data) {
        Subnet model = new Subnet();
        model.id = Long.toString(data.getId());
        model.name = data.getName();
        model.enableDhcp = false;
        model.ipVersion = 4;
        model.networkId = Long.toString(data.getNetwork());
        return model;
    }

}
