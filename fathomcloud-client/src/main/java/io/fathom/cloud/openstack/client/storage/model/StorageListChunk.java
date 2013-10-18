package io.fathom.cloud.openstack.client.storage.model;

import java.util.List;

public class StorageListChunk {

    private final List<StorageObjectInfo> objects;
    private final List<String> subdirs;
    private final String priorLastKey;

    public StorageListChunk(List<StorageObjectInfo> objects, List<String> subdirs, String priorLastKey) {
        super();
        this.objects = objects;
        this.subdirs = subdirs;
        this.priorLastKey = priorLastKey;
    }

    public List<StorageObjectInfo> getObjects() {
        return objects;
    }

    public String getPriorLastKey() {
        return priorLastKey;
    }

    public List<String> getSubdirs() {
        return subdirs;
    }

}
