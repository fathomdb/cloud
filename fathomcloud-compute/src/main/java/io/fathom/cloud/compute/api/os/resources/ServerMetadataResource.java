package io.fathom.cloud.compute.api.os.resources;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.compute.api.os.model.ServerMetadata;
import io.fathom.cloud.compute.services.ComputeServices;
import io.fathom.cloud.compute.services.MetadataServices;
import io.fathom.cloud.protobuf.CloudModel.InstanceData;
import io.fathom.cloud.protobuf.CloudModel.MetadataData;

import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.persist.Transactional;

@Path("/openstack/compute/{project}/servers/{serverId}/metadata")
@Transactional
public class ServerMetadataResource extends ComputeResourceBase {
    private static final Logger log = LoggerFactory.getLogger(ServerMetadataResource.class);

    @Inject
    ComputeServices computeServices;

    @Inject
    MetadataServices metadataServices;

    @PathParam("serverId")
    long instanceId;

    @GET
    public ServerMetadata getMetadata() throws CloudException {
        InstanceData instance = getInstance();

        return buildResponse(instance);
    }

    @GET
    @Path("{key}")
    public ServerMetadata getMetadataKey(@PathParam("key") String key) throws CloudException {
        InstanceData instance = getInstance();

        ServerMetadata response = buildResponse(instance);

        String value = response.metadata.get(key);
        response.metadata.clear();
        if (value != null) {
            response.metadata.put(key, value);
        }

        return response;
    }

    @DELETE
    @Path("{key}")
    public ServerMetadata deleteMetadataKey(@PathParam("key") String key) throws CloudException {
        InstanceData instance = getInstance();

        InstanceData updated = metadataServices.replaceMetadataKey(getProject(), instance.getId(), key, null);

        return buildResponse(updated);
    }

    private ServerMetadata buildResponse(InstanceData instance) {
        ServerMetadata response = new ServerMetadata();
        MetadataData metadata = instance.getMetadata();
        response.metadata = MetadataServices.toMap(metadata);
        return response;
    }

    @PUT
    @Path("{key}")
    public ServerMetadata setMetadataKey(@PathParam("key") String key, ServerMetadata metadata) throws CloudException {
        InstanceData instance = getInstance();

        if (metadata.metadata == null) {
            metadata.metadata = metadata.meta;
        }

        if (metadata.metadata == null) {
            throw new IllegalArgumentException();
        }

        Map<String, String> model = metadata.metadata;

        for (Entry<String, String> entry : model.entrySet()) {
            if (key.equals(entry.getKey())) {
                instance = metadataServices.replaceMetadataKey(getProject(), instance.getId(), key, entry.getValue());
            } else {
                throw new IllegalArgumentException();
            }
        }

        return buildResponse(instance);
    }

    @PUT
    public ServerMetadata replaceMetadata(ServerMetadata metadata) throws CloudException {
        return updateMetadata(metadata, true);
    }

    @POST
    public ServerMetadata mergeMetadata(ServerMetadata metadata) throws CloudException {
        return updateMetadata(metadata, false);
    }

    private ServerMetadata updateMetadata(ServerMetadata metadata, boolean replace) throws CloudException {
        if (metadata.metadata == null) {
            metadata.metadata = metadata.meta;
        }

        if (metadata.metadata == null) {
            throw new IllegalArgumentException();
        }

        Map<String, String> model = metadata.metadata;

        InstanceData instance = getInstance();

        InstanceData updated = metadataServices.replaceMetadata(getProject(), instance.getId(), model, replace);

        return buildResponse(updated);

    }

    private InstanceData getInstance() throws CloudException {
        InstanceData instance = computeServices.findInstance(getProject().getId(), instanceId);
        notFoundIfNull(instance);
        return instance;
    }

}
