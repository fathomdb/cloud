package io.fathom.cloud.compute.metadata;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.compute.services.ComputeServices;
import io.fathom.cloud.protobuf.CloudModel.InstanceData;
import io.fathom.cloud.protobuf.CloudModel.InstanceState;
import io.fathom.cloud.protobuf.CloudModel.KeyPairData;
import io.fathom.cloud.protobuf.CloudModel.MetadataData;
import io.fathom.cloud.protobuf.CloudModel.MetadataEntryData;
import io.fathom.cloud.protobuf.CloudModel.NetworkAddressData;
import io.fathom.cloud.server.resources.FathomCloudResourceBase;
import io.fathom.cloud.services.AuthService;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.net.InetAddresses;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.inject.persist.Transactional;

@Path("/openstack/metadata")
@Transactional
public class MetadataResource extends FathomCloudResourceBase {

    private static final Logger log = LoggerFactory.getLogger(MetadataResource.class);

    @Inject
    ComputeServices computeServices;

    @Inject
    AuthService authServices;

    private InstanceData getInstance() throws CloudException {
        String remoteAddr = httpRequest.getRemoteAddr();

        InetAddress remoteAddress = InetAddresses.forString(remoteAddr);

        InstanceData instance = computeServices.findInstanceByAddress(remoteAddress);
        if (instance == null) {
            throw new WebApplicationException(Status.FORBIDDEN);
        }
        return instance;
    }

    @Path("openstack/{version}/meta_data.json")
    @GET
    @Produces({ JSON })
    public JsonElement getOpenstackMetadata() throws CloudException {
        InstanceData instance = getInstance();

        String identityUri = "http://[fd00::c10d]:8080/openstack/identity/";
        // String identityUri = authServices.getIdentityUri(baseUrl);

        // String uuid = UUID.randomUUID().toString();
        JsonObject o = new JsonObject();
        o.addProperty("uuid", instance.getId());
        o.addProperty("identity_uri", identityUri);

        JsonObject meta = new JsonObject();
        MetadataData metadata = instance.getMetadata();
        for (MetadataEntryData entry : metadata.getEntryList()) {
            meta.addProperty(entry.getKey(), entry.getValue());
        }
        o.add("meta", meta);

        return o;
    }

    @Path("openstack/{version}/secret/{key}")
    @GET
    public byte[] getSecret(@PathParam("key") String key) throws CloudException {
        InstanceData instance = getInstance();

        byte[] secretData = computeServices.getSecret(instance.getProjectId(), instance.getId(), key);
        if (secretData == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        return secretData;
    }

    @Path("openstack/{version}/user_data")
    @GET
    @Produces({ JSON })
    public JsonElement getOpenstackUserData() {
        throw new UnsupportedOperationException();
    }

    @Path("openstack/{version}/peers")
    @GET
    @Produces({ JSON })
    public JsonArray getOpenstackPeers() throws CloudException {
        InstanceData instance = getInstance();

        JsonArray ret = new JsonArray();

        for (InstanceData peer : computeServices.getPeers(instance)) {
            // TODO: Should we skip ourselves??
            if (peer.getId() == instance.getId()) {
                continue;
            }

            // TODO: Return a typed object
            InstanceState state = peer.getInstanceState();
            if (state != null) {
                switch (state) {
                case TERMINATED:
                case STOPPED:
                case STOPPING:
                    continue;
                }
            }

            JsonObject o = new JsonObject();
            JsonArray addresses = new JsonArray();
            for (NetworkAddressData address : peer.getNetwork().getAddressesList()) {
                if (!address.getPublicAddress()) {
                    continue;
                }

                InetAddress inetAddress = InetAddresses.forString(address.getIp());
                if (inetAddress instanceof Inet6Address) {
                    addresses.add(new JsonPrimitive(InetAddresses.toAddrString(inetAddress)));
                }
            }
            o.add("addresses", addresses);
            ret.add(o);
        }

        return ret;
    }

    @GET
    public String getEc2Versions() {
        List<String> versions = Lists.newArrayList();
        versions.add("1.0");
        versions.add("2007-01-19");
        versions.add("2007-03-01");
        versions.add("2007-08-29");
        versions.add("2007-10-10");
        versions.add("2007-12-15");
        versions.add("2008-02-01");
        versions.add("2008-09-01");
        versions.add("2009-04-04");
        versions.add("2011-01-01");
        versions.add("2011-05-01");
        versions.add("2012-01-12");

        return Joiner.on("\n").join(versions);
    }

    @Path("{version}")
    @GET
    public String getEc2Sections() {
        List<String> sections = Lists.newArrayList();
        sections.add("dynamic");
        sections.add("meta-data");
        return Joiner.on("\n").join(sections);
    }

    @Path("{version}/meta-data")
    @GET
    public String getEc2MetadataKeys() {
        List<String> keys = Lists.newArrayList();

        keys.add("ami-id");
        keys.add("ami-launch-index");
        keys.add("ami-manifest-path");
        keys.add("block-device-mapping/");
        keys.add("hostname");
        keys.add("instance-action");
        keys.add("instance-id");
        keys.add("instance-type");
        keys.add("kernel-id");
        keys.add("local-hostname");
        keys.add("local-ipv4");
        keys.add("mac");
        keys.add("metrics/");
        keys.add("network/");
        keys.add("placement/");
        keys.add("product-codes");
        keys.add("profile");
        keys.add("public-hostname");
        keys.add("public-ipv4");
        keys.add("public-keys/");
        keys.add("reservation-id");

        return Joiner.on("\n").join(keys);
    }

    @Path("{version}/meta-data/{key}")
    @GET
    public String getEc2MetadataValue(@PathParam("key") String key) throws CloudException {
        InstanceData instance = getInstance();

        if (key.equals("public-keys")) {
            if (instance.hasKeyPair()) {
                KeyPairData keyPair = instance.getKeyPair();

                String name = keyPair.getKey();

                String s = "0=" + name;
                return s;
            }
        }

        log.warn("Unsupported ec2 metadata key: {}", key);
        throw new WebApplicationException(Status.NOT_FOUND);
    }

    @Path("{version}/meta-data/{key}/{index}/{subkey}")
    @GET
    public String getEc2MetadataArrayValue(@PathParam("key") String key, @PathParam("index") int index,
            @PathParam("subkey") String subkey) throws CloudException {
        InstanceData instance = getInstance();

        if (key.equals("public-keys")) {
            if (subkey.equals("openssh-key")) {
                if (index == 0 && instance.hasKeyPair()) {
                    KeyPairData keyPair = instance.getKeyPair();

                    String s = keyPair.getPublicKey();
                    return s;
                }
            }
        }

        log.warn("Unsupported ec2 metadata key: {}", key);
        throw new WebApplicationException(Status.NOT_FOUND);
    }
}
