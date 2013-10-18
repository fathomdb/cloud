package io.fathom.cloud.compute.api.os.resources;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.compute.api.os.model.Host;
import io.fathom.cloud.compute.api.os.model.Hosts;
import io.fathom.cloud.compute.scheduler.InstanceScheduler;
import io.fathom.cloud.compute.scheduler.SchedulerHost;
import io.fathom.cloud.compute.state.HostStore;
import io.fathom.cloud.protobuf.CloudModel.HostData;
import io.fathom.cloud.server.resources.OpenstackDefaults;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.inject.persist.Transactional;

@Path("/openstack/compute/{project}/os-hosts")
@Transactional
public class HostsResource extends ComputeResourceBase {
    private static final Logger log = LoggerFactory.getLogger(HostsResource.class);

    @Inject
    InstanceScheduler scheduler;

    @Inject
    HostStore hostStore;

    @GET
    @Produces({ JSON })
    public Hosts listHosts() throws CloudException {
        checkDomainAdmin();

        Hosts response = new Hosts();
        response.hosts = Lists.newArrayList();

        for (SchedulerHost host : scheduler.getAllHosts()) {
            response.hosts.add(toModel(host));
        }

        return response;
    }

    // @POST
    // @Produces({ JSON })
    // @Unofficial
    // public Host createHost(Host host) throws CloudException {
    // checkDomainAdmin();
    //
    // HostInfo.Builder hostInfo = HostInfo.newBuilder();
    //
    // for (String address : host.addresses) {
    // hostInfo.addAddress(address);
    // }
    //
    // for (Host.Network network : host.networks) {
    // HostNetworkInfo.Builder net = hostInfo.addNetworksBuilder();
    // net.setKey(network.key);
    // net.setCidr(network.cidr);
    // net.setGateway(network.gateway);
    // net.setPublicNetwork(network.isPublic);
    // }
    //
    // HostInfo created = hostStore.getHosts().create(hostInfo);
    //
    // scheduler.refreshHosts();
    //
    // SchedulerHost schedulerHost = scheduler.findHost(created.getId());
    //
    // return toModel(schedulerHost);
    // }

    private Host toModel(SchedulerHost host) {
        Host model = new Host();
        model.name = Long.toString(host.getId());

        HostData hostData = host.getHostData();

        model.service = "compute";
        model.zone = OpenstackDefaults.DEFAULT_ZONE;

        /*
         * model.addresses = hostInfo.getAddressList();
         * 
         * model.networks = Lists.newArrayList(); for (HostNetworkInfo network :
         * hostInfo.getNetworksList()) { Network net = new Network();
         * 
         * net.key = network.getKey(); net.isPublic =
         * network.getPublicNetwork(); net.cidr = network.getCidr(); net.gateway
         * = network.getGateway(); model.networks.add(net); }
         */

        return model;
    }

}
