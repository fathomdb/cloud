package io.fathom.cloud.compute.api.os.resources;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.compute.api.os.model.Flavor;
import io.fathom.cloud.compute.api.os.model.ListFlavorsResponse;
import io.fathom.cloud.compute.api.os.model.WrappedFlavor;
import io.fathom.cloud.compute.services.Flavors;
import io.fathom.cloud.protobuf.CloudModel.FlavorData;
import io.fathom.cloud.server.resources.FathomCloudResourceBase;

import java.util.Collections;
import java.util.Comparator;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.common.collect.Lists;

@Path("/openstack/compute/{project}/flavors")
@Produces({ MediaType.APPLICATION_JSON })
public class FlavorsResource extends FathomCloudResourceBase {
    @Inject
    Flavors flavors;

    @GET
    public ListFlavorsResponse listFlavors() throws CloudException {
        return listFlavors(false);
    }

    @GET
    @Path("detail")
    public ListFlavorsResponse listFlavorsDetailed() throws CloudException {
        return listFlavors(true);
    }

    @GET
    @Path("{id}")
    public WrappedFlavor showFlavor(@PathParam("id") long id) throws CloudException {
        FlavorData flavor = flavors.find(id);
        notFoundIfNull(flavor);

        WrappedFlavor response = new WrappedFlavor();
        response.flavor = toModel(flavor, true);

        return response;
    }

    private ListFlavorsResponse listFlavors(boolean details) throws CloudException {
        ListFlavorsResponse response = new ListFlavorsResponse();
        response.flavors = Lists.newArrayList();

        for (FlavorData flavor : flavors.list()) {
            response.flavors.add(toModel(flavor, details));
        }

        Collections.sort(response.flavors, new Comparator<Flavor>() {
            @Override
            public int compare(Flavor o1, Flavor o2) {
                return Integer.compare(o1.ram, o2.ram);
            }
        });

        return response;
    }

    Flavor toModel(FlavorData data, boolean details) {
        Flavor flavor = new Flavor();

        flavor.id = data.getId() + "";
        flavor.name = data.getName();

        flavor.ram = data.getRam();
        flavor.disk = data.getDisk();

        flavor.swap = data.getSwap();

        flavor.vcpus = data.getVcpus();
        flavor.ephemeral = data.getEphemeral();

        return flavor;
    }
}
