package io.fathom.cloud.compute.services;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.blobs.BlobData;
import io.fathom.cloud.blobs.TempFile;
import io.fathom.cloud.compute.api.os.model.actions.CreateImageRequest;
import io.fathom.cloud.compute.scheduler.InstanceScheduler;
import io.fathom.cloud.compute.scheduler.SchedulerHost;
import io.fathom.cloud.compute.scheduler.SchedulerHost.SchedulerHostNetwork;
import io.fathom.cloud.compute.state.ComputeRepository;
import io.fathom.cloud.protobuf.CloudModel.InstanceData;
import io.fathom.cloud.protobuf.CloudModel.NetworkAddressData;
import io.fathom.cloud.protobuf.CloudModel.ReservationData;
import io.fathom.cloud.server.auth.Auth;
import io.fathom.cloud.server.auth.Auth.Domain;
import io.fathom.cloud.server.model.Project;
import io.fathom.cloud.services.ImageService;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.net.InetAddresses;
import com.google.inject.persist.Transactional;

@Transactional
@Singleton
public class ComputeServices {
    private static final Logger log = LoggerFactory.getLogger(ComputeServices.class);

    @Inject
    ComputeRepository computeRepository;

    @Inject
    InstanceScheduler scheduler;

    @Inject
    ImageService imageService;

    public List<InstanceData> listInstances(Auth auth, Project project) throws CloudException {
        List<InstanceData> instances = Lists.newArrayList();

        if (project == null) {
            Domain domainAdmin = auth.findDomainWithAdminRole();
            if (domainAdmin == null) {
                throw new WebApplicationException(Status.FORBIDDEN);
            }

            for (long projectId : computeRepository.listInstanceProjects()) {
                if (auth.checkProject(projectId)) {
                    listInstances(instances, projectId);
                }
            }
        } else {
            if (!auth.checkProject(project.getId())) {
                throw new WebApplicationException(Status.NOT_FOUND);
            }

            listInstances(instances, project.getId());
        }

        return instances;
    }

    // private Auth getAuth() {
    // Auth auth = authProvider.get();
    // if (auth == null) {
    // throw new WebApplicationException(Status.UNAUTHORIZED);
    // }
    // return auth;
    // }

    void listInstances(List<InstanceData> dest, long projectId) throws CloudException {
        for (InstanceData instance : computeRepository.getInstances(projectId).list()) {

            switch (instance.getInstanceState()) {
            case TERMINATED:
                continue;

            default:
                break;
            }

            dest.add(instance);
        }
    }

    public InstanceData findInstance(long projectId, long instanceId) throws CloudException {
        return computeRepository.getInstances(projectId).find(instanceId);
    }

    public byte[] getSecret(long projectId, long instanceId, String key) throws CloudException {
        InstanceData instance = findInstance(projectId, instanceId);
        if (instance == null) {
            return null;
        }

        SchedulerHost host = scheduler.findHost(instance.getHostId());
        if (host == null) {
            return null;
        }

        String hostCookie = instance.getHostCookie();
        UUID containerId = UUID.fromString(hostCookie);

        byte[] secretData;
        try {
            secretData = host.getSecret(containerId, key);
        } catch (IOException e) {
            throw new CloudException("Error reading secret", e);
        }
        return secretData;
    }

    public InstanceData findInstanceByAddress(InetAddress address) throws CloudException {
        SchedulerHostNetwork network = scheduler.findHostByAddress(address);
        if (network == null) {
            return null;
        }

        String ip = InetAddresses.toAddrString(address);

        NetworkAddressData addressInfo = computeRepository.getHostIps(network.getHost().getId(), network.getKey())
                .find(ip);
        if (addressInfo == null) {
            return null;
        }

        long projectId = addressInfo.getProjectId();
        long instanceId = addressInfo.getInstanceId();
        if (projectId == 0 || instanceId == 0) {
            return null;
        }

        InstanceData instance = computeRepository.getInstances(projectId).find(instanceId);
        if (instance == null) {
            return null;
        }

        return instance;
        // if (addressInfo.get)
        // VirtualIpData vip =
        // computeRepository.getAllocatedVips(pool.getId()).find(address);
        // if (vip == null) {
        // continue;
        // }
        //
        // if (vip.getProjectId() != project.getId()) {
        // continue;
        // }
        //
        // return new VirtualIp(pool, vip);
        // }
        // }
        // }
        // }
    }

    /**
     * Returns instances in same project; we are experimenting with exposing
     * this through the metadata service
     */
    public List<InstanceData> getPeers(InstanceData instance) throws CloudException {
        return computeRepository.getInstances(instance.getProjectId()).list();
    }

    /**
     * Creates a disk image from a container
     */
    public ImageService.Image createImage(Project project, InstanceData instance, CreateImageRequest request)
            throws IOException, CloudException {
        // TODO: The spec (?) says this call is async.
        // We would probably do better to spend the effort to make it really
        // fast instead (BtrFS?)
        SchedulerHost host = scheduler.findHost(instance.getHostId());
        if (host == null) {
            throw new IllegalStateException();
        }

        String hostCookie = instance.getHostCookie();

        log.warn("createImage is inefficient - we should support side-load");

        Map<String, String> metadata = Maps.newHashMap();
        if (request.metadata != null) {
            metadata.putAll(request.metadata);
        }
        if (request.name != null) {
            metadata.put(ImageService.METADATA_KEY_NAME, request.name);
        }

        metadata.put(ImageService.METADATA_KEY_CONTAINER_FORMAT, "tar");
        metadata.put(ImageService.METADATA_KEY_DISK_FORMAT, "raw");

        UUID containerId = UUID.fromString(hostCookie);
        try (TempFile snapshot = host.createImage(containerId)) {
            ImageService.Image image = imageService.createImage(instance.getProjectId(), metadata);
            BlobData blobData = BlobData.build(snapshot.getFile());
            image = imageService.uploadData(image, blobData);
            return image;
        }
    }

    public ReservationData createReservation(Auth auth, Project project, ReservationData.Builder b)
            throws CloudException {
        return computeRepository.getReservations(project).create(b);
    }

    public InstanceData createInstance(Auth auth, Project project, InstanceData.Builder b) throws CloudException {
        return computeRepository.getInstances(project.getId()).create(b);
    }

}
