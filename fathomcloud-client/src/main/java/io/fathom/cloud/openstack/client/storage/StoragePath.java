package io.fathom.cloud.openstack.client.storage;

import io.fathom.cloud.openstack.client.RestClientException;
import io.fathom.cloud.openstack.client.storage.model.StorageObjectInfo;

import java.util.List;

import com.google.common.base.Charsets;
import com.google.common.io.ByteSource;

public class StoragePath {
    final OpenstackStorageClient client;

    final String bucket;
    final String path;

    public StoragePath(OpenstackStorageClient client, String bucket, String path) {
        this.client = client;
        this.bucket = bucket;
        this.path = path;
    }

    public StorageObject read() throws RestClientException {
        StorageObject object = client.getObject(bucket + "/" + path);
        return object;
    }

    public void write(String value) throws RestClientException {
        client.putFile(bucket + "/" + path, ByteSource.wrap(value.getBytes(Charsets.UTF_8)));
    }

    public void write(byte[] value) throws RestClientException {
        client.putFile(bucket + "/" + path, ByteSource.wrap(value));
    }

    public StoragePath child(String s) {
        String childPath;
        if (path.endsWith("/")) {
            childPath = path + s;
        } else {
            childPath = path + "/" + s;
        }
        return new StoragePath(client, bucket, childPath);
    }

    public List<StorageObjectInfo> listChildren(boolean recursive) throws RestClientException {
        String listPath = path;
        if (!listPath.endsWith("/")) {
            listPath += "/";
        }
        return client.listChildren(bucket, listPath, recursive ? null : "/");
    }

    public boolean delete() throws RestClientException {
        try {
            client.delete(bucket + "/" + path);
            return true;
        } catch (RestClientException e) {
            if (e.is(404)) {
                return false;
            }
            throw new RestClientException("Error deleting cloud file", e);
        }
    }

    public void ensureBucket() throws RestClientException {
        client.createBucket(bucket);
    }

    public String stripPrefix(String objectPath) {
        String ret = objectPath;
        if (ret.startsWith(path)) {
            ret = ret.substring(path.length());

            if (ret.startsWith("/") && !path.endsWith("/")) {
                ret = ret.substring(1);
            }
        }
        return ret;
    }

    public String getBucket() {
        return bucket;
    }

    public String getKey() {
        return path;
    }
}
