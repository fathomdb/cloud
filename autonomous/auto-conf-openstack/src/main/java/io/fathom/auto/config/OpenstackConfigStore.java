package io.fathom.auto.config;

import io.fathom.auto.openstack.secrets.OpenstackSecretKeys;
import io.fathom.cloud.openstack.client.OpenstackClient;
import io.fathom.cloud.openstack.client.RestClientException;
import io.fathom.cloud.openstack.client.storage.OpenstackStorageClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenstackConfigStore extends ConfigStore {

    private static final Logger log = LoggerFactory.getLogger(OpenstackConfigStore.class);

    private final String basePath;
    private final String serviceKey;

    private final String clusterKey;

    private final OpenstackClient client;

    final SecretKeys secretKeys;

    private final ConfigStoreProvider parent;

    public OpenstackConfigStore(ConfigStoreProvider parent, OpenstackClient client, String clusterKey, String basePath,
            String serviceKey) {
        this.parent = parent;
        this.client = client;
        this.clusterKey = clusterKey;
        if (basePath.startsWith("/")) {
            basePath = basePath.substring(0);
        }
        if (!basePath.equals("") && !basePath.endsWith("/")) {
            basePath = basePath + "/";
        }

        if (basePath.startsWith("/")) {
            throw new IllegalArgumentException();
        }

        this.basePath = basePath;
        this.serviceKey = serviceKey;

        this.secretKeys = new OpenstackSecretKeys(client);
    }

    @Override
    public void init() {
        try {
            ensureBucket();
        } catch (RestClientException e) {
            log.warn("Error checking that bucket exists", e);
        }
    }

    // private StoragePath storagePath;

    // public StoragePath getStoragePath() {
    // StoragePath storagePath = null;
    // while (storagePath == null) {
    // log.info("Querying openstack metadata");
    //
    // try {
    // storagePath = getStoragePath0();
    // break;
    // } catch (RestClientException e) {
    // // TODO: Special case this one?
    // log.error("Error reading metadata", e);
    // } catch (Exception e) {
    // log.error("Error reading metadata", e);
    // }
    // TimeSpan.seconds(5).sleep();
    // }
    //
    // return storagePath;
    // }
    //
    // public StoragePath getStoragePath0() throws RestClientException {
    // if (storagePath == null) {
    // String bucket = "__services";
    //
    // String path = basePath + serviceKey;
    // storagePath = new StoragePath(getStorageClient(), bucket, path);
    //
    // storagePath.ensureBucket();
    // }
    // return storagePath;
    // }

    public void ensureBucket() throws RestClientException {
        getConfigRoot0().ensureBucket();
    }

    OpenstackStorageClient getStorageClient() throws RestClientException {
        return client.getStorage();
    }

    @Override
    protected OpenstackConfigPath getConfigRoot0() {
        String bucket = "__services";

        String path = basePath + serviceKey;

        return new OpenstackConfigPath(this, bucket, path);
    }

    @Override
    public ConfigPath getSharedPath(String key) {
        String bucket = "__services";

        String path = basePath + key;
        return new OpenstackConfigPath(this, bucket, path);
    }

    @Override
    public String getClusterKey() {
        return clusterKey;
    }

    @Override
    public SecretKeys getSecretKeys() {
        return secretKeys;
    }

    @Override
    public String getInstanceProperty(String key) {
        return parent.getInstanceProperty(key);
    }

}
