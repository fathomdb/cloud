package io.fathom.auto.config;

import io.fathom.auto.TimeSpan;
import io.fathom.auto.config.ConfigEntry;
import io.fathom.auto.config.ConfigPath;
import io.fathom.auto.locks.OpenstackPseudoLock;
import io.fathom.cloud.openstack.client.RestClientException;
import io.fathom.cloud.openstack.client.storage.StorageObject;
import io.fathom.cloud.openstack.client.storage.StoragePath;
import io.fathom.cloud.openstack.client.storage.model.StorageObjectInfo;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.locks.Lock;

import com.google.common.collect.Lists;

public class OpenstackConfigPath extends ConfigPath {

    private final OpenstackConfigStore store;
    private final String bucket;
    private final String key;

    public OpenstackConfigPath(OpenstackConfigStore store, String bucket, String key) {
        this.store = store;
        this.bucket = bucket;
        this.key = key;
    }

    @Override
    public ConfigPath child(String s) {
        String childKey;
        if (key.endsWith("/")) {
            childKey = key + s;
        } else {
            childKey = key + "/" + s;
        }

        return new OpenstackConfigPath(store, bucket, childKey);
    }

    private StoragePath getStoragePath() throws RestClientException {
        StoragePath storagePath = new StoragePath(store.getStorageClient(), bucket, key);
        return storagePath;
    }

    @Override
    public Iterable<ConfigEntry> listChildren() throws IOException {
        List<ConfigEntry> children = Lists.newArrayList();

        try {
            StoragePath path = getStoragePath();

            for (StorageObjectInfo o : path.listChildren(false)) {
                String name = path.stripPrefix(o.name);
                long version = o.getLastModifiedTimestamp();
                ConfigEntry child = new ConfigEntry(name, version);
                children.add(child);
            }
        } catch (RestClientException e) {
            throw new IOException("Error listing children", e);
        }

        return children;
    }

    // @Override
    // public String readChild(String name) throws IOException {
    // StoragePath childPath = path.child(name);
    // try (StorageObject storageObject = childPath.read()) {
    // if (storageObject == null) {
    // return null;
    // }
    // return storageObject.getAsString();
    //
    // } catch (RestClientException e) {
    // throw new IOException("Error reading config file", e);
    // }
    // }

    @Override
    public String read() throws IOException {
        try {
            StoragePath path = getStoragePath();

            try (StorageObject storageObject = path.read()) {
                if (storageObject == null) {
                    return null;
                }
                return storageObject.getAsString();
            }
        } catch (RestClientException e) {
            throw new IOException("Error reading config file", e);
        }
    }

    @Override
    public void write(String contents) throws IOException {
        try {
            StoragePath path = getStoragePath();

            path.write(contents);
        } catch (RestClientException e) {
            throw new IOException("Error writing config file", e);
        }
    }

    @Override
    public void delete() throws IOException {
        try {
            StoragePath path = getStoragePath();

            path.delete();
        } catch (RestClientException e) {
            throw new IOException("Error deleting config file", e);
        }
    }

    @Override
    public Lock buildLock() {
        TimeSpan lockTimeout = TimeSpan.minutes(10);
        TimeSpan lockPollInterval = TimeSpan.seconds(5);

        OpenstackPseudoLock lock = new OpenstackPseudoLock(this, lockTimeout, lockPollInterval);
        return lock;
    }

    public void ensureBucket() throws RestClientException {
        getStoragePath().ensureBucket();
    }

}
