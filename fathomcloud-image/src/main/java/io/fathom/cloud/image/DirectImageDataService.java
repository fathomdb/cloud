package io.fathom.cloud.image;

import io.fathom.cloud.blobs.BlobData;
import io.fathom.cloud.blobs.BlobStore;
import io.fathom.cloud.blobs.BlobStoreFactory;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.fathomdb.utils.Hex;
import com.google.inject.persist.Transactional;
import com.google.protobuf.ByteString;

@Singleton
@Transactional
public class DirectImageDataService implements ImageDataService {
    @Inject
    BlobStoreFactory blobStoreFactory;

    @Override
    public String storeImageFile(BlobData data) throws IOException {
        BlobStore blobStore = getBlobStore();

        blobStore.put(data);

        return Hex.toHex(data.getHash().toByteArray());
    }

    BlobStore getBlobStore() throws IOException {
        return blobStoreFactory.get("images");
    }

    @Override
    public BlobData getImageFile(String cookie) throws IOException {
        BlobStore blobStore = getBlobStore();

        ByteString key = ByteString.copyFrom(Hex.fromHex(cookie));

        BlobData blobData = blobStore.find(key);

        return blobData;
    }
}
