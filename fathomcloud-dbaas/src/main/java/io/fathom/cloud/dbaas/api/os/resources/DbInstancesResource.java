package io.fathom.cloud.dbaas.api.os.resources;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.dbaas.DbaasServiceImpl;
import io.fathom.cloud.dbaas.api.os.model.DbInstanceList;
import io.fathom.cloud.dbaas.api.os.model.WrappedDbInstance;
import io.fathom.cloud.server.model.Project;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.inject.persist.Transactional;

@Path("/openstack/dbaas/{project}/instances")
@Transactional
public class DbInstancesResource extends DbaasResourceBase {
    private static final Logger log = LoggerFactory.getLogger(DbInstancesResource.class);

    @Inject
    DbaasServiceImpl dbaasService;

    @GET
    public DbInstanceList getInstances() {
        DbInstanceList instances = new DbInstanceList();
        instances.instances = Lists.newArrayList();
        return instances;
    }

    @POST
    public WrappedDbInstance createDbInstance(WrappedDbInstance request) throws CloudException {
        // {"instance": {"volume": {"size": 1}, "flavorRef": "1", "name":
        // "db1"}}
        Project project = getProject();

        throw new UnsupportedOperationException();
    }
}