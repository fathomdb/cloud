package io.fathom.cloud.blobs;

import java.io.IOException;

public interface BlobStoreFactory {
    BlobStore get(String key) throws IOException;
}
