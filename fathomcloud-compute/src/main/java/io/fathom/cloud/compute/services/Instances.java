package io.fathom.cloud.compute.services;

import io.fathom.cloud.Clock;
import io.fathom.cloud.CloudException;
import io.fathom.cloud.compute.scheduler.InstanceScheduler;
import io.fathom.cloud.compute.scheduler.SchedulerHost;
import io.fathom.cloud.compute.state.ComputeRepository;
import io.fathom.cloud.protobuf.CloudModel.InstanceData;
import io.fathom.cloud.server.model.Project;
import io.fathom.cloud.state.NumberedItemCollection;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathomdb.TimeSpan;
import com.google.common.base.Strings;
import com.google.inject.persist.Transactional;

@Singleton
@Transactional
public class Instances {

    private static final Logger log = LoggerFactory.getLogger(Instances.class);

    @Inject
    ComputeRepository repository;

    @Inject
    InstanceScheduler scheduler;

    public void purgeDeletedInstances(TimeSpan age) throws CloudException {
        for (long projectId : repository.listInstanceProjects()) {
            try {
                purgeDeletedInstances(projectId, age);
            } catch (Exception e) {
                log.error("Error while purging instances for project: " + projectId, e);
            }
        }
    }

    public void purgeDeletedInstances(long projectId, TimeSpan age) throws CloudException {
        for (InstanceData instance : repository.getInstances(projectId).list()) {
            boolean delete = false;

            switch (instance.getInstanceState()) {
            case TERMINATED: {
                Date deletedAt = Clock.toDate(instance.getTerminatedAt());
                if (age.hasTimedOut(deletedAt)) {
                    delete = true;
                }
                break;
            }
            default:
                continue;
            }

            if (delete) {
                log.info("Purging instance: {}", instance.getId());

                try {
                    purgeInstance(instance);
                } catch (Exception e) {
                    log.error("Error while purging instance: " + instance.getId(), e);
                }
            }
        }
    }

    private void purgeInstance(InstanceData instance) throws IOException, CloudException {
        SchedulerHost host = scheduler.findHost(instance.getHostId());
        if (host == null) {
            throw new IllegalStateException("No host for instance");
        }

        String hostCookie = instance.getHostCookie();
        if (Strings.isNullOrEmpty(hostCookie)) {
            throw new IllegalStateException("No container for instance");
        }

        UUID containerId = UUID.fromString(hostCookie);
        host.purgeInstance(containerId);

        // TODO: Should we delete entirely? Maybe after a certain amount of
        // time? Maybe as part of an accounting sweep?
    }

    public InstanceData updateInstance(Project project, long id, String name) throws CloudException {
        NumberedItemCollection<InstanceData> store = repository.getInstances(project.getId());
        InstanceData instance = store.find(id);
        if (instance == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        InstanceData.Builder b = InstanceData.newBuilder(instance);
        b.setName(name);

        return store.update(b);
    }
}
