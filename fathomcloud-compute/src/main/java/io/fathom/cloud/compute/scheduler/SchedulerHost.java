package io.fathom.cloud.compute.scheduler;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.blobs.BlobData;
import io.fathom.cloud.blobs.TempFile;
import io.fathom.cloud.compute.networks.IpRange;
import io.fathom.cloud.compute.services.DatacenterManager;
import io.fathom.cloud.protobuf.CloudModel.HostData;
import io.fathom.cloud.protobuf.CloudModel.InstanceData;
import io.fathom.cloud.services.ImageKey;
import io.fathom.cloud.services.ImageService;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.google.common.base.Strings;

public abstract class SchedulerHost {
    public static final String SECRET_TOKEN = "token";

    final long id;
    final HostData hostData;

    public SchedulerHost(HostData hostData) {
        this.id = hostData.getId();
        this.hostData = hostData;
    }

    public long getId() {
        return id;
    }

    public abstract UUID createContainer(InstanceData instance, ImageService.Image image) throws CloudException;

    public abstract ConfigurationOperation startConfiguration() throws CloudException;

    public abstract void startContainer(UUID containerId) throws CloudException;

    public abstract boolean stopContainer(UUID containerId) throws CloudException;

    public abstract boolean hasImage(ImageKey imageKey) throws IOException, CloudException;

    public abstract void uploadImage(ImageKey imageKey, BlobData imageData) throws IOException, CloudException;

    public HostData getHostData() {
        return hostData;
    }

    public InetAddress getIpAddress() {
        IpRange range = IpRange.parse(hostData.getCidr());
        return range.getAddress();
    }

    public static interface SchedulerHostNetwork {
        InetAddress getGateway();

        IpRange getIpRange();

        boolean isPublicNetwork();

        String getKey();

        SchedulerHost getHost();
    }

    public abstract List<SchedulerHostNetwork> getNetworks();

    public String getNetworkDevice() {
        String networkDevice = hostData.getNetworkDevice();
        if (Strings.isNullOrEmpty(networkDevice)) {
            throw new IllegalArgumentException();
        }
        return networkDevice;
    }

    public abstract byte[] getSecret(UUID containerId, String key) throws IOException, CloudException;

    public abstract void setSecret(UUID containerId, String key, byte[] data) throws IOException, CloudException;

    public abstract TempFile createImage(UUID containerId) throws IOException, CloudException;

    public abstract void purgeInstance(UUID containerId) throws IOException, CloudException;

    public abstract DatacenterManager getDatacenterManager();

    public abstract String fetchUrl(URI uri) throws IOException;

}
