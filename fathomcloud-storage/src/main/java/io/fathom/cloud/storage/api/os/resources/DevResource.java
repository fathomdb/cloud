package io.fathom.cloud.storage.api.os.resources;

import javax.ws.rs.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.persist.Transactional;

@Path("/openstack/storage/dev")
@Transactional
public class DevResource extends ObjectstoreResourceBase {
    private static final Logger log = LoggerFactory.getLogger(DevResource.class);

    // @GET
    // @Path("repair")
    // public Response repair() throws CloudException {
    // ReplicatedBlobStore replicatedBlobStore = (ReplicatedBlobStore)
    // blobStore;
    //
    // ReplicaRepair repair = new ReplicaRepair(
    // replicatedBlobStore.getCluster());
    //
    // String prefix = "";
    // repair.repair(prefix);
    //
    // ResponseBuilder response = Response.ok();
    // return response.build();
    // }

}
