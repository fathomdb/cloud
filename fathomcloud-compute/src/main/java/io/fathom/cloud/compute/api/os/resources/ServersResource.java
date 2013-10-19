package io.fathom.cloud.compute.api.os.resources;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.compute.actions.StartInstancesAction;
import io.fathom.cloud.compute.actions.StopInstancesAction;
import io.fathom.cloud.compute.api.os.model.Address;
import io.fathom.cloud.compute.api.os.model.Addresses;
import io.fathom.cloud.compute.api.os.model.Flavor;
import io.fathom.cloud.compute.api.os.model.Image;
import io.fathom.cloud.compute.api.os.model.SecurityGroup;
import io.fathom.cloud.compute.api.os.model.SecurityGroupList;
import io.fathom.cloud.compute.api.os.model.Server;
import io.fathom.cloud.compute.api.os.model.ServerList;
import io.fathom.cloud.compute.api.os.model.VolumeAttachments;
import io.fathom.cloud.compute.api.os.model.WrappedServer;
import io.fathom.cloud.compute.api.os.model.actions.AddFloatingIpRequest;
import io.fathom.cloud.compute.api.os.model.actions.AddSecurityGroupRequest;
import io.fathom.cloud.compute.api.os.model.actions.CreateImageRequest;
import io.fathom.cloud.compute.api.os.model.actions.RemoveFloatingIpRequest;
import io.fathom.cloud.compute.api.os.model.actions.RemoveSecurityGroupRequest;
import io.fathom.cloud.compute.networks.IpRanges;
import io.fathom.cloud.compute.services.ComputeServices;
import io.fathom.cloud.compute.services.Flavors;
import io.fathom.cloud.compute.services.Instances;
import io.fathom.cloud.compute.services.IpPools;
import io.fathom.cloud.compute.services.MetadataServices;
import io.fathom.cloud.compute.services.SecurityGroups;
import io.fathom.cloud.compute.services.SshKeyPairs;
import io.fathom.cloud.compute.state.ComputeRepository;
import io.fathom.cloud.protobuf.CloudModel.FlavorData;
import io.fathom.cloud.protobuf.CloudModel.InstanceData;
import io.fathom.cloud.protobuf.CloudModel.KeyPairData;
import io.fathom.cloud.protobuf.CloudModel.NetworkAddressData;
import io.fathom.cloud.protobuf.CloudModel.ReservationData;
import io.fathom.cloud.protobuf.CloudModel.SecurityGroupData;
import io.fathom.cloud.server.auth.Auth;
import io.fathom.cloud.server.model.Project;
import io.fathom.cloud.services.ImageService;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.net.InetAddresses;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.persist.Transactional;

@Path("/openstack/compute/{project}/servers")
@Transactional
public class ServersResource extends ComputeResourceBase {
    private static final Logger log = LoggerFactory.getLogger(ServersResource.class);

    @Inject
    SecurityGroups securityGroups;

    @Inject
    Instances instances;

    @Inject
    Flavors flavors;

    @Inject
    Provider<StartInstancesAction> startInstancesActionProvider;

    @Inject
    Provider<StopInstancesAction> stopInstancesActionProvider;

    @Inject
    ComputeRepository instanceStateStore;

    @Inject
    ComputeServices computeServices;

    @Inject
    ImageService imageService;

    @Inject
    SshKeyPairs keypairs;

    @Inject
    Gson gson;

    @Inject
    IpPools ipPools;

    @GET
    @Path("{id}")
    public WrappedServer doServerGet(@PathParam("id") String id) throws CloudException {
        InstanceData instance = getInstance(id);

        WrappedServer response = new WrappedServer();

        ReservationData reservation = getReservation(instance.getReservationId());

        response.server = toModel(reservation, instance, true);
        return response;
    }

    @PUT
    @Path("{id}")
    public WrappedServer doServerPut(@PathParam("id") String id, WrappedServer request) throws CloudException {
        InstanceData instance = getInstance(id);

        if (request.server == null) {
            request.server = new Server();
        }

        if (request.server.name != null && !Objects.equal(instance.getName(), request.server.name)) {
            instance = instances.updateInstance(getProject(), instance.getId(), request.server.name);
        }

        WrappedServer response = new WrappedServer();

        ReservationData reservation = getReservation(instance.getReservationId());

        response.server = toModel(reservation, instance, true);
        return response;
    }

    @POST
    @Path("{id}/action")
    public Response doAction(@PathParam("id") String id, JsonObject jsonRequest) throws CloudException, IOException {
        InstanceData instance = getInstance(id);

        for (Entry<String, JsonElement> entry : jsonRequest.entrySet()) {
            String key = entry.getKey();
            if (key.equals("addFloatingIp")) {
                AddFloatingIpRequest request = gson.fromJson(entry.getValue(), AddFloatingIpRequest.class);

                return addFloatingIp(instance, request);
            } else if (key.equals("removeFloatingIp")) {
                RemoveFloatingIpRequest request = gson.fromJson(entry.getValue(), RemoveFloatingIpRequest.class);

                return removeFloatingIp(instance, request);
            } else if (key.equals("addSecurityGroup")) {
                AddSecurityGroupRequest request = gson.fromJson(entry.getValue(), AddSecurityGroupRequest.class);

                return addRemoveSecurityGroup(instance, request.name, false);
            } else if (key.equals("removeSecurityGroup")) {
                RemoveSecurityGroupRequest request = gson.fromJson(entry.getValue(), RemoveSecurityGroupRequest.class);

                return addRemoveSecurityGroup(instance, request.name, true);
            } else if (key.equals("createImage")) {
                CreateImageRequest request = gson.fromJson(entry.getValue(), CreateImageRequest.class);

                return createImage(instance, request);
            } else {
                throw new IllegalArgumentException("Unknown action: " + key);
            }
        }
        throw new IllegalArgumentException();

    }

    private Response addRemoveSecurityGroup(InstanceData instance, String name, boolean remove) throws CloudException {
        Project project = getProject();

        Long id = Long.valueOf(name);
        SecurityGroupData sg = securityGroups.find(project, id);

        // SecurityGroupData sg = securityGroups.find(project, request.name);
        notFoundIfNull(sg);

        securityGroups.addRemoveSecurityGroup(project, instance.getId(), sg, remove);

        return Response.accepted().build();
    }

    private Response createImage(InstanceData instance, CreateImageRequest request) throws IOException, CloudException {
        Project project = getProject();

        ImageService.Image image = computeServices.createImage(project, instance, request);

        String location = imageService.getUrl(httpRequest, image.getId());

        return Response.ok().header(HttpHeaders.LOCATION, location).build();
    }

    private Response removeFloatingIp(InstanceData instance, RemoveFloatingIpRequest request) throws CloudException {
        Project project = getProject();

        ipPools.detachFloatingIp(project, instance, request);

        return Response.ok().build();
    }

    protected Response addFloatingIp(InstanceData instance, AddFloatingIpRequest request) throws CloudException {
        Project project = getProject();

        ipPools.attachFloatingIp(project, instance, request);

        return Response.ok().build();
    }

    @DELETE
    @Path("{id}")
    public Response shutdownServer(@PathParam("id") String id) throws CloudException {
        InstanceData instance = getInstance(id);

        StopInstancesAction action = stopInstancesActionProvider.get();
        action.project = getProject();
        action.instances = Lists.newArrayList();
        action.instances.add(instance);
        action.go();

        ResponseBuilder response = Response.noContent();
        return response.build();
    }

    @GET
    @Path("{id}/os-volume_attachments")
    public VolumeAttachments listVolumeAttachments(@PathParam("id") String id) throws CloudException {
        VolumeAttachments response = new VolumeAttachments();

        response.volumeAttachments = Lists.newArrayList();

        log.warn("os-volume_attachments is stub-implemented");

        return response;
    }

    @GET
    @Path("{id}/os-security-groups")
    public SecurityGroupList listSecurityGroups(@PathParam("id") String id) throws CloudException {
        Project project = getProject();

        InstanceData instance = getInstance(id);

        SecurityGroupList response = new SecurityGroupList();

        response.securityGroups = Lists.newArrayList();

        for (long sgId : instance.getSecurityGroupIdList()) {
            SecurityGroupData data = securityGroups.find(getProject(), sgId);
            if (data == null) {
                log.warn("Cannot find sg: {}", sgId);
                continue;
            }
            SecurityGroup model = SecurityGroupsResource.toModel(project, data, true);
            response.securityGroups.add(model);
        }

        return response;
    }

    private InstanceData getInstance(String id) throws CloudException {
        InstanceData instance = instanceStateStore.getInstances(getProject().getId()).find(Long.valueOf(id));
        if (instance == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }
        return instance;
    }

    private ReservationData getReservation(long id) throws CloudException {
        ReservationData r = instanceStateStore.getReservations(getProject()).find(id);
        if (r == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }
        return r;
    }

    private ServerList listServers(boolean details) throws CloudException {
        Auth auth = getAuth();

        boolean allTenants = httpRequest.getParameter("all_tenants") != null;

        Project filterProject = getProject();
        if (allTenants) {
            filterProject = null;
        }

        List<InstanceData> instances = computeServices.listInstances(auth, filterProject);

        Map<Long, ReservationData> reservations = Maps.newHashMap();

        ServerList response = new ServerList();
        response.servers = Lists.newArrayList();
        for (InstanceData instance : instances) {
            ReservationData reservation = reservations.get(instance.getReservationId());
            if (reservation == null) {
                reservation = getReservation(instance.getReservationId());
                reservations.put(instance.getReservationId(), reservation);
            }
            response.servers.add(toModel(reservation, instance, details));
        }

        return response;
    }

    @GET
    @Path("detail")
    public ServerList listDetails() throws CloudException {
        return listServers(true);
    }

    @GET
    public ServerList list() throws CloudException {
        return listServers(false);
    }

    @POST
    @Produces({ JSON })
    public Response launchServer(WrappedServer request) throws CloudException {
        StartInstancesAction action = startInstancesActionProvider.get();

        // action.user = getUser();
        action.project = getProject();
        action.auth = getAuth();

        FlavorData flavor;
        {
            long flavorId = OpenstackIds.toFlavorId(request.server.flavorRef);
            flavor = flavors.find(flavorId);
            if (flavor == null) {
                throw new IllegalArgumentException();
            }
        }

        action.minCount = request.server.minCount;
        if (action.minCount == 0) {
            action.minCount = 1;
        }
        action.maxCount = request.server.maxCount;
        if (action.maxCount == 0) {
            action.maxCount = 1;
        }

        if (action.maxCount != 1) {
            // Not clear what the response should be in this case...
            throw new UnsupportedOperationException();
        }

        ImageService.Image image;
        {

            long imageId = OpenstackIds.toImageId(request.server.imageRef);
            image = imageService.findImage(getProject(), imageId);
            if (image == null) {
                throw new IllegalArgumentException();
            }
        }

        {
            ReservationData.Builder reservation = ReservationData.newBuilder();
            // TODO: Copy image?
            reservation.setImageId(image.getId());

            action.reservationTemplate = reservation.build();
        }

        {
            InstanceData.Builder instance = InstanceData.newBuilder();
            instance.setName(request.server.name);

            if (request.server.keyName != null) {
                KeyPairData keypair = keypairs.findKeyPair(getProject(), request.server.keyName);
                if (keypair == null) {
                    throw new IllegalArgumentException();
                }
                instance.setKeyPair(keypair);
            }

            instance.setImageId(image.getId());

            instance.setFlavor(flavor);

            if (request.server.securityGroups != null && !request.server.securityGroups.isEmpty()) {
                SecurityGroupDictionary dictionary = new SecurityGroupDictionary(securityGroups.list(getProject()));

                for (SecurityGroup securityGroup : request.server.securityGroups) {
                    String name = securityGroup.name;

                    SecurityGroupData data = dictionary.getByName(name);
                    if (data == null) {
                        throw new IllegalArgumentException("Security group not found: " + name);
                    }

                    instance.addSecurityGroupId(data.getId());
                }
            }

            action.instanceTemplate = instance.build();
        }

        StartInstancesAction.Result result = action.go();

        WrappedServer response = new WrappedServer();

        if (result.instances.size() != 1) {
            throw new IllegalStateException();
        }

        Map<Long, ReservationData> reservations = Maps.newHashMap();

        for (InstanceData instance : result.instances) {
            ReservationData reservation = reservations.get(instance.getReservationId());
            if (reservation == null) {
                reservation = getReservation(instance.getReservationId());
                reservations.put(instance.getReservationId(), reservation);
            }

            response.server = toModel(reservation, instance, false);

            // TODO: adminPass??
        }

        return Response.status(Status.ACCEPTED).entity(response).build();
    }

    private Server toModel(ReservationData reservation, InstanceData instance, boolean details) {
        Server server = new Server();

        server.id = "" + instance.getId();
        server.links = Lists.newArrayList();
        server.name = instance.getName();

        server.flavor = new Flavor();
        if (instance.hasFlavor()) {
            server.flavor.id = "" + instance.getFlavor().getId();
        }

        server.tenant_id = "" + instance.getProjectId();

        if (details) {
            server.progress = 0;
            // server.extensionTaskState = "";
            server.extensionPowerState = 1;

            server.created = new Date(instance.getLaunchTime());

            server.addresses = new Addresses();
            server.addresses.privateAddresses = Lists.newArrayList();
            server.addresses.publicAddresses = Lists.newArrayList();

            server.image = new Image();
            server.image.id = "" + instance.getImageId();

            server.metadata = MetadataServices.toMap(instance.getMetadata());

            if (instance.hasNetwork()) {
                InetAddress bestIpv4 = null;
                InetAddress bestIpv6 = null;

                for (NetworkAddressData addressData : instance.getNetwork().getAddressesList()) {
                    String addressString = addressData.getIp();
                    InetAddress address = InetAddresses.forString(addressString);

                    Address xml = new Address();
                    xml.address = addressString;

                    boolean publicAddress = true;
                    if (address instanceof Inet4Address) {
                        xml.version = 4;

                        if (bestIpv4 == null) {
                            bestIpv4 = address;
                        }
                    } else {
                        xml.version = 6;

                        if (bestIpv6 == null) {
                            bestIpv6 = address;
                        }
                    }

                    if (publicAddress) {
                        server.addresses.publicAddresses.add(xml);
                    } else {
                        server.addresses.privateAddresses.add(xml);
                    }
                }

                if (bestIpv4 != null) {
                    boolean publishIpv4 = true;
                    if (!IpRanges.isPublic(bestIpv4) && bestIpv6 != null) {
                        publishIpv4 = false;
                    }

                    if (publishIpv4) {
                        server.accessIPv4 = InetAddresses.toAddrString(bestIpv4);
                    }
                }

                if (bestIpv6 != null) {
                    server.accessIPv6 = InetAddresses.toAddrString(bestIpv6);
                }
            }

            switch (instance.getInstanceState()) {
            case TERMINATED:
            case STOPPED:
                server.status = "DELETED";
                break;

            case RUNNING:
            case SHUTTING_DOWN:
            case STOPPING:
                server.status = "ACTIVE";
                break;

            case PENDING:
                server.status = "ERROR";
                // server.status = "BUILD";
                break;

            default:
                log.warn("Unhandled state {}", instance.getInstanceState());

                server.status = "UNKNOWN";
                break;
            }

            // "accessIPv4": "",
            // "accessIPv6": "",
            // "addresses": {
            // "private": [
            // {
            // "addr": "192.168.0.3",
            // "version": 4
            // }
            // ]
            // },
            // "created": "2012-09-07T16:56:37Z",
            // "flavor": {
            // "id": "1",
            // "links": [
            // {
            // "href": "http://openstack.example.com/openstack/flavors/1",
            // "rel": "bookmark"
            // }
            // ]
            // },
            // "hostId":
            // "16d193736a5cfdb60c697ca27ad071d6126fa13baeb670fc9d10645e",
            // "id": "05184ba3-00ba-4fbc-b7a2-03b62b884931",
            // "image": {
            // "id": "70a599e0-31e7-49b7-b260-868f441e862b",
            // "links": [
            // {
            // "href":
            // "http://openstack.example.com/openstack/images/70a599e0-31e7-49b7-b260-868f441e862b",
            // "rel": "bookmark"
            // }
            // ]
            // },
            // "links": [
            // {
            // "href":
            // "http://openstack.example.com/v2/openstack/servers/05184ba3-00ba-4fbc-b7a2-03b62b884931",
            // "rel": "self"
            // },
            // {
            // "href":
            // "http://openstack.example.com/openstack/servers/05184ba3-00ba-4fbc-b7a2-03b62b884931",
            // "rel": "bookmark"
            // }
            // ],
            // "metadata": {
            // "My Server Name": "Apache1"
            // },
            // "name": "new-server-test",
            // "progress": 0,
            // "status": "ACTIVE",
            // "tenant_id": "openstack",
            // "updated": "2012-09-07T16:56:37Z",
            // "user_id": "fake"
        }
        return server;
    }

}
