package io.fathom.cloud.compute.scheduler;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.blobs.BlobData;
import io.fathom.cloud.compute.actions.StartInstancesAction.StartInstanceData;
import io.fathom.cloud.compute.services.ComputeDerivedMetadata;
import io.fathom.cloud.compute.services.ComputeServices;
import io.fathom.cloud.compute.services.SecurityGroups;
import io.fathom.cloud.protobuf.CloudModel.InstanceData;
import io.fathom.cloud.protobuf.CloudModel.InstanceState;
import io.fathom.cloud.protobuf.CloudModel.NetworkAddressData;
import io.fathom.cloud.protobuf.CloudModel.SecurityGroupData;
import io.fathom.cloud.server.auth.Auth;
import io.fathom.cloud.server.model.Project;
import io.fathom.cloud.services.ImageKey;
import io.fathom.cloud.services.ImageService;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.net.InetAddresses;

public class StartInstanceOperation extends SchedulerOperation {
    private static final Logger log = LoggerFactory.getLogger(StartInstanceOperation.class);

    @Inject
    ImageService imageService;

    @Inject
    SecurityGroups securityGroups;

    @Inject
    ComputeServices computeServices;

    @Inject
    ComputeDerivedMetadata derivedMetadata;

    InstanceData instance;

    private Project project;

    private String token;

    private Auth auth;

    public void init(Auth auth, Project project, StartInstanceData start) {
        this.auth = auth;
        this.project = project;
        this.token = start.token;
        this.instance = start.instanceInfo;
    }

    @Override
    public boolean run() throws CloudException, IOException {
        List<SecurityGroupData> instanceSecurityGroups = securityGroups.getSecurityGroups(project, instance);

        SchedulerHost host = scheduler.findHost(instance.getHostId());
        if (host == null) {
            throw new IllegalStateException();
        }

        // Upload the image
        ImageService.Image image;
        {
            image = imageService.findImage(project, instance.getImageId());
            if (image == null) {
                throw new IllegalStateException("Cannot find image: " + instance.getImageId());
            }

            ImageKey imageKey = image.getUniqueKey();

            if (!host.hasImage(imageKey)) {
                // TODO: Support side-load

                BlobData imageData = imageService.getImageBlob(image);

                host.uploadImage(imageKey, imageData);
            }
        }

        instance = services.assignIps(project, host, instance);

        UUID containerId = host.createContainer(instance, image);

        services.updateInstance(instance, InstanceData.newBuilder().setHostCookie(containerId.toString()));

        updateSecurityGroupIpsets();

        try (ConfigurationOperation config = host.startConfiguration()) {
            config.configureFirewall(instance, instanceSecurityGroups);
            config.applyChanges();
        }

        host.setSecret(containerId, SchedulerHost.SECRET_TOKEN, token.getBytes(Charsets.UTF_8));
        host.startContainer(containerId);

        instance = services.updateInstance(instance, InstanceData.newBuilder().setInstanceState(InstanceState.RUNNING));

        derivedMetadata.instanceUpdated(project, instance);

        return true;
    }

    private void updateSecurityGroupIpsets() throws CloudException, IOException {
        Set<Long> launchSgs = Sets.newHashSet(instance.getSecurityGroupIdList());

        Multimap<Long, InstanceData> sgInstances = HashMultimap.create();

        for (InstanceData peer : computeServices.listInstances(auth, project)) {
            boolean valid = true;
            switch (peer.getInstanceState()) {
            case STOPPED:
            case TERMINATED:
                valid = false;
                break;
            }

            if (!valid) {
                continue;
            }
            for (Long peerSg : peer.getSecurityGroupIdList()) {
                if (!launchSgs.contains(peerSg)) {
                    continue;
                }

                sgInstances.put(peerSg, peer);
            }
        }

        for (Long instanceSg : instance.getSecurityGroupIdList()) {
            sgInstances.put(instanceSg, instance);
        }

        Set<Long> doneHostIds = Sets.newHashSet();
        for (InstanceData instance : sgInstances.values()) {
            long hostId = instance.getHostId();
            if (doneHostIds.contains(hostId)) {
                continue;
            }

            SchedulerHost host = scheduler.findHost(hostId);
            if (host == null) {
                log.error("Unable to find host: " + hostId);
                continue;
            }

            try (ConfigurationOperation config = host.startConfiguration()) {
                for (long sg : sgInstances.keySet()) {
                    Set<String> ips = Sets.newHashSet();
                    for (InstanceData i : sgInstances.get(sg)) {
                        for (NetworkAddressData nai : i.getNetwork().getAddressesList()) {
                            InetAddress address = InetAddresses.forString(nai.getIp());
                            if (address instanceof Inet6Address) {
                                ips.add(InetAddresses.toAddrString(address));
                            }
                        }
                    }
                    config.configureIpset(sg, ips);
                }
                config.applyChanges();
            }

            doneHostIds.add(hostId);
        }
    }
}
