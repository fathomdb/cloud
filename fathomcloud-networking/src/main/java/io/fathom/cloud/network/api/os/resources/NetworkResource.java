package io.fathom.cloud.network.api.os.resources;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.network.NetworkService;
import io.fathom.cloud.network.ProtobufFilter;
import io.fathom.cloud.network.api.os.models.Network;
import io.fathom.cloud.network.api.os.models.Networks;
import io.fathom.cloud.network.api.os.models.WrappedNetwork;
import io.fathom.cloud.protobuf.NetworkingModel.NetworkData;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

@Path("/openstack/network/{project}/v2.0/networks")
public class NetworkResource extends NetworkResourceBase {
    private static final Logger log = LoggerFactory.getLogger(NetworkResource.class);

    @Inject
    NetworkService networkService;

    @Inject
    HttpServletRequest request;

    @GET
    @Produces({ JSON })
    public Networks listNetworks() throws CloudException {
        List<NetworkData> networks = networkService.listNetworks(getAuth());

        List<ProtobufFilter> filters = Lists.newArrayList();

        Map<String, String[]> parameterMap = request.getParameterMap();
        for (Entry<String, String[]> entry : parameterMap.entrySet()) {
            String key = entry.getKey();
            String[] values = entry.getValue();

            for (String value : values) {
                if (key.equals("name")) {
                    filters.add(new ProtobufFilter(NetworkData.getDescriptor().findFieldByName("name"), value));
                } else if (key.equals("shared")) {
                    filters.add(new ProtobufFilter(NetworkData.getDescriptor().findFieldByName("shared"), Boolean
                            .valueOf(value)));
                } else if (key.equals("tenant_id")) {
                    filters.add(new ProtobufFilter(NetworkData.getDescriptor().findFieldByName("project"), Long
                            .valueOf(value)));
                } else {
                    log.warn("Unknown key: {}", key);
                }
            }
        }

        Networks model = new Networks();
        model.networks = Lists.newArrayList();
        for (NetworkData network : networks) {
            boolean match = true;
            for (ProtobufFilter filter : filters) {
                if (!filter.matches(network)) {
                    match = false;
                    break;
                }
            }

            if (match) {
                model.networks.add(toModel(network));
            }
        }
        return model;
    }

    @GET
    @Path("{id}")
    @Produces({ JSON })
    public WrappedNetwork getNetwork(@PathParam("id") long id) throws CloudException {
        NetworkData data = networkService.findNetwork(getAuth(), id);
        if (data == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        WrappedNetwork ret = new WrappedNetwork();
        ret.network = toModel(data);
        return ret;
    }

    @PUT
    @Path("{id}")
    @Produces({ JSON })
    public WrappedNetwork updateNetwork(@PathParam("id") long id, WrappedNetwork request) throws CloudException {
        Network network = request.network;
        NetworkData.Builder b = toBuilder(network);

        NetworkData data = networkService.updateNetwork(getAuth(), id, b);
        if (data == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        WrappedNetwork ret = new WrappedNetwork();
        ret.network = toModel(data);
        return ret;
    }

    @DELETE
    @Path("{id}")
    @Produces({ JSON })
    public Response deleteNetwork(@PathParam("id") long id, WrappedNetwork request) throws CloudException {
        Status status = networkService.deleteNetwork(getAuth(), id);

        if (status != null) {
            throw new WebApplicationException(status);
        }

        ResponseBuilder response = Response.noContent();
        return response.build();
    }

    @POST
    @Produces({ JSON })
    public WrappedNetwork createNetwork(WrappedNetwork request) throws CloudException {
        Network network = request.network;

        NetworkData.Builder b = toBuilder(network);

        NetworkData data = networkService.createNetwork(getAuth(), b);

        WrappedNetwork ret = new WrappedNetwork();
        ret.network = toModel(data);
        return ret;
    }

    private NetworkData.Builder toBuilder(Network network) {
        if (network == null) {
            throw new IllegalArgumentException();
        }
        NetworkData.Builder b = NetworkData.newBuilder();
        if (network.name != null) {
            b.setName(network.name);
        }

        if (network.adminStateUp != null) {
            b.setAdminStateUp(network.adminStateUp);
        }

        if (network.shared != null) {
            b.setShared(network.shared);
        }

        if (network.routerExternal != null) {
            b.setRouterExternal(network.routerExternal);
        }

        return b;
    }

    private Network toModel(NetworkData data) {
        Network model = new Network();
        model.status = data.getStatus();
        model.subnets = Lists.newArrayList();
        model.name = data.getName();
        model.adminStateUp = data.getAdminStateUp();
        model.routerExternal = data.getRouterExternal();
        model.tenantId = Long.toString(data.getProject());
        model.id = Long.toString(data.getId());
        model.shared = data.getShared();

        return model;
    }

}
