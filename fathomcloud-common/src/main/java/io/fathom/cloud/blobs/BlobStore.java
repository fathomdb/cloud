package io.fathom.cloud.blobs;

import java.io.IOException;

import com.google.protobuf.ByteString;

public interface BlobStore {
    BlobData find(ByteString key) throws IOException;

    void put(BlobData data) throws IOException;

    Iterable<ByteString> listWithPrefix(String prefix) throws IOException;

    boolean has(ByteString key, boolean checkCache) throws IOException;
}
