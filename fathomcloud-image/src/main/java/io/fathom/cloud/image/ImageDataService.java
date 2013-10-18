package io.fathom.cloud.image;

import io.fathom.cloud.blobs.BlobData;

import java.io.IOException;

import com.google.inject.ImplementedBy;

@ImplementedBy(DirectImageDataService.class)
public interface ImageDataService {
    String storeImageFile(BlobData data) throws IOException;

    BlobData getImageFile(String cookie) throws IOException;
}