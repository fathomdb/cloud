package io.fathom.cloud.services;

import io.fathom.cloud.services.ImageService.Image;

public interface ImageImports {

    public interface Metadata {

        String getChecksum();

    }

    Metadata getImageMetadata(String imageUrl) throws Exception;

    Image importImage(long projectId, String imageUrl) throws Exception;
}
