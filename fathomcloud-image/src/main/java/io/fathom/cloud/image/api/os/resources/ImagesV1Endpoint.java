package io.fathom.cloud.image.api.os.resources;

import io.fathom.cloud.Clock;
import io.fathom.cloud.CloudException;
import io.fathom.cloud.blobs.BlobData;
import io.fathom.cloud.image.ImageDataService;
import io.fathom.cloud.image.ImageServiceImpl;
import io.fathom.cloud.image.ImageServiceImpl.ImageImpl;
import io.fathom.cloud.image.api.os.model.ImageListResponse;
import io.fathom.cloud.image.api.os.model.WrappedImage;
import io.fathom.cloud.protobuf.CloudCommons.KeyValueData;
import io.fathom.cloud.protobuf.ImageModel.ImageData;
import io.fathom.cloud.protobuf.ImageModel.ImageLocation;
import io.fathom.cloud.services.ImageService;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathomdb.utils.Hex;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.persist.Transactional;
import com.google.protobuf.ByteString;

@Path("/openstack/images/v1/images")
@Transactional
public class ImagesV1Endpoint extends ImageResourceBase {
    private static final Logger log = LoggerFactory.getLogger(ImagesV1Endpoint.class);

    @Inject
    ImageServiceImpl imageService;

    @Inject
    ImageDataService imageDataService;

    private ImageData findImage(String id) throws CloudException {
        ImageImpl image = imageService.findImage(getProject(), Long.valueOf(id));
        if (image == null) {
            return null;
        }

        return image.getData();
    }

    // @PUT
    // @Path("{imageId}/file")
    // public Response updateContent(File file) throws CloudException,
    // IOException {
    // ImageData image = findImage();
    // if (image == null) {
    // throw new WebApplicationException(Status.NOT_FOUND);
    // }
    //
    // BlobData data = BlobData.build(file);
    //
    // String stored = imageDataService.storeImageFile(image, data);
    //
    // ImageLocation.Builder b = ImageLocation.newBuilder();
    // b.setStored(stored);
    //
    // imageService.setImageLocation(image.getId(), data.size(),
    // data.getHash(), b.build());
    //
    // return Response.status(Status.NO_CONTENT).build();
    // }
    //
    // private ImageData findImage() throws CloudException {
    // return imageService.findImage(Long.valueOf(imageId));
    // }

    private static final String IMAGE_META_PREFIX = "x-image-meta-";

    @POST
    @Consumes({ "application/octet-stream" })
    public Response createImage(File file) throws CloudException, IOException {
        if (file == null) {
            throw new IllegalArgumentException("No content supplied");
        }

        ImageImpl image;
        {
            Map<String, String> metadata = extractHeaders();
            if (metadata.containsKey("size")) {
                long size = Long.valueOf(metadata.get(ImageService.METADATA_KEY_SIZE));
                if (size != file.length()) {
                    throw new IllegalArgumentException();
                }
            }
            metadata.put("size", "" + Long.valueOf(file.length()));

            image = imageService.createImage(getProject().getId(), metadata);
        }

        {
            BlobData data = BlobData.build(file);
            image = imageService.uploadData(image, data);
        }

        WrappedImage result = new WrappedImage();
        result.image = toModel(image.getData());

        return Response.status(Status.CREATED).entity(result).type(MediaType.APPLICATION_JSON_TYPE).build();
    }

    private Map<String, String> extractHeaders() {
        Map<String, String> metadata = Maps.newHashMap();

        Enumeration<String> headerNames = httpRequest.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();

            // Header names are case-insensitive
            String normalized = headerName.toLowerCase();
            if (normalized.startsWith(IMAGE_META_PREFIX)) {
                String key = headerName.substring(IMAGE_META_PREFIX.length());
                String value = httpRequest.getHeader(headerName);

                metadata.put(key, value);
            }
        }

        return metadata;
    }

    @GET
    @Path("detail")
    @Produces({ JSON })
    public ImageListResponse doImageDetailsGet() throws CloudException {
        ImageListResponse response = new ImageListResponse();

        response.images = Lists.newArrayList();

        ImageFilter filter = new ImageFilter();
        filter.name = httpRequest.getParameter("name");

        for (ImageService.Image data : imageService.listImages(getProject(), filter)) {
            response.images.add(toModel(data));
        }

        return response;
    }

    @DELETE
    @Path("{id}")
    public Response deleteImage(@PathParam("id") String id) throws CloudException, IOException {
        ImageData image = findImage(id);
        if (image == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        imageService.deleteImage(getProject(), image.getId());

        return Response.status(Status.NO_CONTENT).build();
    }

    @PUT
    @Path("{id}")
    public Response updateImage(@PathParam("id") String id, File content) throws CloudException, IOException {
        if (content != null && content.length() != 0) {
            throw new UnsupportedOperationException();
        }

        ImageData image = findImage(id);
        if (image == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        // TODO: Some
        // -H 'x-image-meta-protected: True'
        // -H 'x-glance-registry-purge-props: false'

        Map<String, String> metadata = extractHeaders();

        image = imageService.updateImage(getProject(), image.getId(), metadata);

        // ResponseBuilder response = Response.ok();
        // setHttpHeaders(image, response);
        // return response.build();

        WrappedImage result = new WrappedImage();
        result.image = toModel(image);
        return Response.status(Status.CREATED).entity(result).type(MediaType.APPLICATION_JSON_TYPE).build();
    }

    @HEAD
    @Path("{id}")
    public Response getImageInfo(@PathParam("id") String id) throws CloudException, IOException {
        ImageData image = findImage(id);
        if (image == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        ResponseBuilder response = Response.ok();
        setHttpHeaders(image, response);
        return response.build();
    }

    @GET
    @Path("{id}")
    public Response getImage(@PathParam("id") String id) throws CloudException, IOException {
        ImageData image = findImage(id);
        if (image == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        ResponseBuilder response;
        ImageLocation imageLocation = image.getLocation();
        if (imageLocation.hasStored()) {
            String cookie = imageLocation.getStored();

            BlobData blob = imageDataService.getImageFile(cookie);

            response = Response.ok().entity(blob.asEntity());

            ByteString md5 = blob.getHash();
            response.header("Content-MD5", Hex.toHex(md5.toByteArray()));
        } else {
            response = Response.status(Status.NO_CONTENT);
        }

        setHttpHeaders(image, response);
        return response.build();
    }

    private void setHttpHeaders(ImageData image, ResponseBuilder response) {
        // TODO: We could fix this up to be less hacky, but this works for now

        response.header("x-image-meta-id", "" + image.getId());
        response.header("x-image-meta-uri", imageService.getUrl(httpRequest, image.getId()));

        if (image.hasName()) {
            response.header("x-image-meta-name", image.getName());
        }

        if (image.hasDiskFormat()) {
            response.header("x-image-meta-disk_format", image.getDiskFormat());
        }

        if (image.hasContainerFormat()) {
            response.header("x-image-meta-container_format", image.getContainerFormat());
        }

        if (image.hasImageSize()) {
            response.header("x-image-meta-size", "" + image.getImageSize());
        }
        if (image.hasImageChecksum()) {
            response.header("x-image-meta-checksum", Hex.toHex(image.getImageChecksum().toByteArray()));
        }

        if (image.hasCreatedAt()) {
            response.header("x-image-meta-created_at", Clock.toDate(image.getCreatedAt()));
        }
        if (image.hasUpdatedAt()) {
            response.header("x-image-meta-updated_at", Clock.toDate(image.getUpdatedAt()));
        }

        if (image.hasDeletedAt()) {
            response.header("x-image-meta-deleted_at", Clock.toDate(image.getDeletedAt()));
        }

        String status = "queued";
        if (image.hasImageState()) {
            status = image.getImageState().toString().toLowerCase();
        }
        response.header("x-image-meta-status", status);

        if (image.hasIsPublic()) {
            response.header("x-image-meta-is_public", Boolean.toString(image.getIsPublic()));
        }

        response.header("x-image-meta-protected", image.getIsProtected());
        response.header("x-image-meta-owner", "" + image.getOwnerProject());

        if (image.hasAttributes()) {
            for (KeyValueData kv : image.getAttributes().getUserAttributesList()) {
                response.header("x-image-meta-property-" + kv.getKey(), kv.getValue());
            }
        }

        // x-image-meta-is-public true

        // x-image-meta-min-ram 256
        // x-image-meta-min-disk 0
        // x-image-meta-owner null
        // x-image-meta-property-distro Ubuntu 10.04 LTS
    }
}