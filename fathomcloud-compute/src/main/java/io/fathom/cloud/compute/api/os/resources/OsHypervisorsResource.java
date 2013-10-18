package io.fathom.cloud.compute.api.os.resources;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.compute.api.os.model.Hypervisor;
import io.fathom.cloud.compute.api.os.model.HypervisorStatistics;
import io.fathom.cloud.compute.api.os.model.Hypervisors;
import io.fathom.cloud.compute.scheduler.InstanceScheduler;
import io.fathom.cloud.compute.scheduler.SchedulerHost;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.net.InetAddresses;
import com.google.inject.persist.Transactional;

@Path("/openstack/compute/{project}/os-hypervisors")
@Transactional
public class OsHypervisorsResource extends ComputeResourceBase {
    private static final Logger log = LoggerFactory.getLogger(OsHypervisorsResource.class);

    @Inject
    InstanceScheduler scheduler;

    @GET
    public Hypervisors listHypervisors() throws CloudException {
        checkDomainAdmin();

        Hypervisors response = new Hypervisors();
        response.hypervisors = Lists.newArrayList();

        for (SchedulerHost host : scheduler.getAllHosts()) {
            response.hypervisors.add(toModel(host));
        }

        return response;
    }

    @GET
    @Path("statistics")
    public HypervisorStatistics getHypervisorsStatistics() throws CloudException {
        checkDomainAdmin();

        HypervisorStatistics response = new HypervisorStatistics();

        Hypervisor stats = null;

        for (SchedulerHost host : scheduler.getAllHosts()) {
            if (stats == null) {
                stats = toModel(host);
                stats.count = 1L;
            } else {
                Hypervisor model = toModel(host);
                add(stats, model);
            }
        }

        if (stats == null) {
            stats = new Hypervisor();
            stats.count = 0L;
        }

        response.hypervisor_statistics = stats;

        return response;
    }

    @GET
    @Path("detail")
    public Hypervisors listHypervisorsDetail() throws CloudException {
        checkDomainAdmin();

        Hypervisors response = new Hypervisors();
        response.hypervisors = Lists.newArrayList();

        for (SchedulerHost host : scheduler.getAllHosts()) {
            response.hypervisors.add(toModel(host));
        }
        return response;
    }

    private Hypervisor toModel(SchedulerHost host) {
        Hypervisor model = new Hypervisor();
        model.id = host.getId();

        model.hypervisor_hostname = InetAddresses.toAddrString(host.getIpAddress());

        model.memory_mb = 0L;
        model.memory_mb_used = 0L;

        model.local_gb = 0L;
        model.local_gb_used = 0L;

        return model;
    }

    private void add(Hypervisor stats, Hypervisor model) {
        stats.count++;

        stats.memory_mb += model.memory_mb;
        stats.memory_mb_used += model.memory_mb_used;

        stats.local_gb += model.local_gb;
        stats.local_gb_used += model.local_gb_used;
    }

}
