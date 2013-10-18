package io.fathom.cloud.blobs.replicated;

import io.fathom.cloud.blobs.BlobStore;
import io.fathom.cloud.blobs.BlobStoreFactory;
import io.fathom.cloud.mq.RequestExecutor;

import java.io.IOException;

public class StorageNode {
    final String key;
    final BlobStoreFactory blobStoreFactory;
    final RequestExecutor requestExecutor;

    public StorageNode(String key, BlobStoreFactory blobStoreFactory, RequestExecutor requestExecutor) {
        this.key = key;
        this.blobStoreFactory = blobStoreFactory;
        this.requestExecutor = requestExecutor;
    }

    public String getKey() {
        return key;
    }

    BlobStore getBlobStore(String storeKey) throws IOException {
        return blobStoreFactory.get(storeKey);
    }
}
