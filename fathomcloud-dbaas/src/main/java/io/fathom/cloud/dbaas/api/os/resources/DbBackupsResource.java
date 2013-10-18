package io.fathom.cloud.dbaas.api.os.resources;

import io.fathom.cloud.dbaas.DbaasServiceImpl;
import io.fathom.cloud.dbaas.api.os.model.DbBackupList;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.inject.persist.Transactional;

@Path("/openstack/dbaas/{project}/backups")
@Transactional
public class DbBackupsResource extends DbaasResourceBase {
    private static final Logger log = LoggerFactory.getLogger(DbBackupsResource.class);

    @Inject
    DbaasServiceImpl dbaasService;

    @GET
    public DbBackupList getInstances() {
        DbBackupList backups = new DbBackupList();
        backups.backups = Lists.newArrayList();
        return backups;
    }

    // private static final String IMAGE_META_PREFIX = "x-image-meta-";
    //
    // @POST
    // @Consumes({ "application/octet-stream" })
    // public Response createImage(File file) throws CloudException, IOException
    // {
    // if (file == null) {
    // throw new IllegalArgumentException("No content supplied");
    // }
    //
    // ImageImpl image;
    // {
    // Map<String, String> metadata = extractHeaders();
    // if (metadata.containsKey("size")) {
    // long size = Long.valueOf(metadata.get(DbaasService.METADATA_KEY_SIZE));
    // if (size != file.length()) {
    // throw new IllegalArgumentException();
    // }
    // }
    // metadata.put("size", "" + Long.valueOf(file.length()));
    //
    // image = imageService.createImage(getProject().getId(), metadata);
    // }
    //
    // {
    // BlobData data = BlobData.build(file);
    // image = imageService.uploadData(image, data);
    // }
    //
    // WrappedInstance result = new WrappedInstance();
    // result.image = toModel(image.getData());
    //
    // return
    // Response.status(Status.CREATED).entity(result).type(MediaType.APPLICATION_JSON_TYPE).build();
    // }
    //
    // private Map<String, String> extractHeaders() {
    // Map<String, String> metadata = Maps.newHashMap();
    //
    // Enumeration<String> headerNames = httpRequest.getHeaderNames();
    // while (headerNames.hasMoreElements()) {
    // String headerName = headerNames.nextElement();
    //
    // // Header names are case-insensitive
    // String normalized = headerName.toLowerCase();
    // if (normalized.startsWith(IMAGE_META_PREFIX)) {
    // String key = headerName.substring(IMAGE_META_PREFIX.length());
    // String value = httpRequest.getHeader(headerName);
    //
    // metadata.put(key, value);
    // }
    // }
    //
    // return metadata;
    // }
    //
    // @GET
    // @Path("detail")
    // @Produces({ JSON })
    // public ImageListResponse doImageDetailsGet() throws CloudException {
    // ImageListResponse response = new ImageListResponse();
    //
    // response.images = Lists.newArrayList();
    //
    // ImageFilter filter = new ImageFilter();
    // filter.name = httpRequest.getParameter("name");
    //
    // for (DbaasService.Instance data : imageService.listImages(getProject(),
    // filter)) {
    // response.images.add(toModel(data));
    // }
    //
    // return response;
    // }
    //
    // @DELETE
    // @Path("{id}")
    // public Response deleteImage(@PathParam("id") String id) throws
    // CloudException, IOException {
    // ImageData image = findImage(id);
    // if (image == null) {
    // throw new WebApplicationException(Status.NOT_FOUND);
    // }
    //
    // imageService.deleteImage(getProject(), image.getId());
    //
    // return Response.status(Status.NO_CONTENT).build();
    // }
    //
    // @PUT
    // @Path("{id}")
    // public Response updateImage(@PathParam("id") String id, File content)
    // throws CloudException, IOException {
    // if (content != null && content.length() != 0) {
    // throw new UnsupportedOperationException();
    // }
    //
    // ImageData image = findImage(id);
    // if (image == null) {
    // throw new WebApplicationException(Status.NOT_FOUND);
    // }
    //
    // // TODO: Some
    // // -H 'x-image-meta-protected: True'
    // // -H 'x-glance-registry-purge-props: false'
    //
    // Map<String, String> metadata = extractHeaders();
    //
    // image = imageService.updateImage(getProject(), image.getId(), metadata);
    //
    // // ResponseBuilder response = Response.ok();
    // // setHttpHeaders(image, response);
    // // return response.build();
    //
    // WrappedInstance result = new WrappedInstance();
    // result.image = toModel(image);
    // return
    // Response.status(Status.CREATED).entity(result).type(MediaType.APPLICATION_JSON_TYPE).build();
    // }
    //
    // @HEAD
    // @Path("{id}")
    // public Response getImageInfo(@PathParam("id") String id) throws
    // CloudException, IOException {
    // ImageData image = findImage(id);
    // if (image == null) {
    // throw new WebApplicationException(Status.NOT_FOUND);
    // }
    //
    // ResponseBuilder response = Response.ok();
    // setHttpHeaders(image, response);
    // return response.build();
    // }
    //
    // @GET
    // @Path("{id}")
    // public Response getImage(@PathParam("id") String id) throws
    // CloudException, IOException {
    // ImageData image = findImage(id);
    // if (image == null) {
    // throw new WebApplicationException(Status.NOT_FOUND);
    // }
    //
    // ResponseBuilder response;
    // ImageLocation imageLocation = image.getLocation();
    // if (imageLocation.hasStored()) {
    // String cookie = imageLocation.getStored();
    //
    // BlobData blob = imageDataService.getImageFile(cookie);
    //
    // response = Response.ok().entity(blob.asEntity());
    //
    // ByteString md5 = blob.getHash();
    // response.header("Content-MD5", Hex.toHex(md5.toByteArray()));
    // } else {
    // response = Response.status(Status.NO_CONTENT);
    // }
    //
    // setHttpHeaders(image, response);
    // return response.build();
    // }
    //
    // private void setHttpHeaders(ImageData image, ResponseBuilder response) {
    // // TODO: We could fix this up to be less hacky, but this works for now
    //
    // response.header("x-image-meta-id", "" + image.getId());
    //
    // if (image.hasName()) {
    // response.header("x-image-meta-name", image.getName());
    // }
    //
    // if (image.hasDiskFormat()) {
    // response.header("x-image-meta-disk_format", image.getDiskFormat());
    // }
    //
    // if (image.hasContainerFormat()) {
    // response.header("x-image-meta-container_format",
    // image.getContainerFormat());
    // }
    //
    // if (image.hasImageSize()) {
    // response.header("x-image-meta-size", "" + image.getImageSize());
    // }
    // if (image.hasImageChecksum()) {
    // response.header("x-image-meta-checksum",
    // Hex.toHex(image.getImageChecksum().toByteArray()));
    // }
    //
    // if (image.hasCreatedAt()) {
    // response.header("x-image-meta-created_at",
    // Clock.toDate(image.getCreatedAt()));
    // }
    // if (image.hasUpdatedAt()) {
    // response.header("x-image-meta-updated_at",
    // Clock.toDate(image.getUpdatedAt()));
    // }
    //
    // if (image.hasDeletedAt()) {
    // response.header("x-image-meta-deleted_at",
    // Clock.toDate(image.getDeletedAt()));
    // }
    //
    // if (image.hasImageState()) {
    // String status = image.getImageState().toString().toLowerCase();
    // response.header("x-image-meta-status", status);
    // }
    //
    // if (image.hasIsPublic()) {
    // response.header("x-image-meta-is_public",
    // Boolean.toString(image.getIsPublic()));
    // }
    //
    // response.header("x-image-meta-protected", image.getIsProtected());
    // response.header("x-image-meta-owner", "" + image.getOwnerProject());
    //
    // if (image.hasAttributes()) {
    // for (KeyValueData kv : image.getAttributes().getUserAttributesList()) {
    // response.header("x-image-meta-property-" + kv.getKey(), kv.getValue());
    // }
    // }
    //
    // // x-image-meta-uri
    // //
    // http://glance.example.com/v1/images/71c675ab-d94f-49cd-a114-e12490b328d9
    // // x-image-meta-min-ram 256
    // // x-image-meta-min-disk 0
    // // x-image-meta-owner null
    // // x-image-meta-property-distro Ubuntu 10.04 LTS
    // }
}