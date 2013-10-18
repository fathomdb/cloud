package io.fathom.cloud.services;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.blobs.BlobData;
import io.fathom.cloud.server.model.Project;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

public interface ImageService {
    public static final String METADATA_KEY_NAME = "name";
    public static final String METADATA_KEY_SIZE = "size";
    public static final String METADATA_KEY_CONTAINER_FORMAT = "container_format";
    public static final String METADATA_KEY_DISK_FORMAT = "disk_format";

    public interface Image {
        long getId();

        ImageKey getUniqueKey();

        String getName();

        String getStatus();

        String getChecksum();
    }

    Image findImage(Project project, long imageId) throws CloudException;

    BlobData getImageBlob(Image image) throws IOException;

    Image uploadData(Image image, BlobData src) throws IOException, CloudException;

    Image createImage(long projectId, Map<String, String> metadata) throws CloudException;

    String getUrl(HttpServletRequest request, long imageId);

    List<Image> listImages(Project project) throws CloudException;
}
