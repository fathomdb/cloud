package io.fathom.cloud.compute.api.os.resources;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.compute.api.os.model.QuotaSet;
import io.fathom.cloud.compute.api.os.model.WrappedQuotaSet;
import io.fathom.cloud.server.resources.FathomCloudResourceBase;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import com.google.inject.persist.Transactional;

@Path("/openstack/compute/{project}/os-quota-sets")
@Transactional
public class QuotaSetsResource extends FathomCloudResourceBase {
    @GET
    @Path("{id}")
    @Produces({ JSON })
    public WrappedQuotaSet doLimitsGet(@PathParam("id") String projectId) throws CloudException {
        warnStub();

        WrappedQuotaSet response = new WrappedQuotaSet();

        response.quotaSet = new QuotaSet();
        fillEmptyQuotas(response.quotaSet);
        response.quotaSet.id = projectId;

        return response;
    }

    @GET
    @Path("{id}/defaults")
    @Produces({ JSON })
    public WrappedQuotaSet getDefaultLimits(@PathParam("id") String projectId) throws CloudException {
        warnStub();

        WrappedQuotaSet response = new WrappedQuotaSet();

        response.quotaSet = new QuotaSet();

        fillEmptyQuotas(response.quotaSet);

        response.quotaSet.id = "defaults";

        return response;
    }

    @PUT
    @Path("{id}")
    @Produces({ JSON })
    public WrappedQuotaSet updateQuotaSet(@PathParam("id") String projectId) throws CloudException {
        warnStub();

        WrappedQuotaSet response = new WrappedQuotaSet();

        response.quotaSet = new QuotaSet();

        fillEmptyQuotas(response.quotaSet);

        response.quotaSet.id = projectId;

        return response;
    }

    private void fillEmptyQuotas(QuotaSet quotaSet) {
        quotaSet.cores = -1;
        quotaSet.fixed_ips = -1;
        quotaSet.floating_ips = -1;
        quotaSet.injected_file_content_bytes = -1;
        quotaSet.injected_file_path_bytes = -1;
        quotaSet.injected_files = -1;
        quotaSet.instances = -1;
        quotaSet.key_pairs = -1;
        quotaSet.metadata_items = -1;
        quotaSet.ram = -1;
        quotaSet.security_groups = -1;
        quotaSet.security_group_rules = -1;

        quotaSet.volumes = -1;
        quotaSet.snapshots = -1;
        quotaSet.gigabytes = -1;
    }

}
