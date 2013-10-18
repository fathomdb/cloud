package io.fathom.cloud.image.imports;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.blobs.BlobData;
import io.fathom.cloud.blobs.TempFile;
import io.fathom.cloud.image.ImageMetadata;
import io.fathom.cloud.openstack.client.RestClientException;
import io.fathom.cloud.services.ImageImports;
import io.fathom.cloud.services.ImageService;
import io.fathom.cloud.services.ImageService.Image;
import io.fathom.http.HttpClient;
import io.fathom.http.jre.JreHttpClient;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ImageImportsImpl implements ImageImports {

    private static final Logger log = LoggerFactory.getLogger(ImageImportsImpl.class);

    @Inject
    ImageService imageService;

    @Override
    public Metadata getImageMetadata(String imageUrl) throws Exception {
        HttpClient httpClient = JreHttpClient.create();
        URI imageUri = URI.create(imageUrl);
        ImportImageClient imageClient = new ImportImageClient(httpClient, imageUri);

        final ImageImportMetadata imageMetadata = imageClient.getMetadata(imageUri);

        return new Metadata() {
            @Override
            public String getChecksum() {
                return imageMetadata.checksum;
            }
        };
    }

    @Override
    public Image importImage(long projectId, String imageUrl) throws IOException, CloudException, RestClientException {
        HttpClient httpClient = JreHttpClient.create();
        URI imageUri = URI.create(imageUrl);
        ImportImageClient imageClient = new ImportImageClient(httpClient, imageUri);

        log.info("Downloading metadata {}", imageUri);

        ImageImportMetadata imageMetadata = imageClient.getMetadata(imageUri);

        Map<String, String> metadata = imageMetadata.metadata;

        metadata.put(ImageMetadata.KEY_IMAGE_SOURCE, imageUri.toString());

        log.info("Downloading image {}", imageUri);

        // TODO: It would be nice to avoid re-downloading the same image.
        // But... the metadata may be different. Punt for now!
        // Image found = null;
        // for (Image image : imageService.listImages(project))) {
        // String checksum = image.getChecksum();
        // if (checksum.equalsIgnoreCase(imageMetadata.checksum)) {
        // log.warn("Found matching image: {}", found);
        //
        // // TODO: Compare metadata??
        // found = image;
        //
        // break;
        // }
        // }

        try (TempFile tempImage = imageClient.downloadImage(imageUri, imageMetadata)) {
            log.info("Creating image record");
            Image image = imageService.createImage(projectId, metadata);

            log.info("Uploading image data");
            BlobData blobData = BlobData.build(tempImage.getFile());
            image = imageService.uploadData(image, blobData);

            return image;
        }
    }
}
