package io.fathom.cloud.image.api.os.resources;

//
//import java.io.File;
//import java.io.IOException;
//
//import javax.inject.Inject;
//import javax.ws.rs.Consumes;
//import javax.ws.rs.GET;
//import javax.ws.rs.POST;
//import javax.ws.rs.PUT;
//import javax.ws.rs.Path;
//import javax.ws.rs.PathParam;
//import javax.ws.rs.Produces;
//import javax.ws.rs.WebApplicationException;
//import javax.ws.rs.core.Response;
//import javax.ws.rs.core.Response.ResponseBuilder;
//import javax.ws.rs.core.Response.Status;
//
//import org.joda.time.format.DateTimeFormatter;
//import org.joda.time.format.ISODateTimeFormat;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import io.fathom.cloud.CloudException;
//import io.fathom.cloud.blobs.BlobData;
//import io.fathom.cloud.images.ImageDataService;
//import io.fathom.cloud.images.ImageService;
//import io.fathom.cloud.images.api.os.model.Image;
//import io.fathom.cloud.images.api.os.model.ImageListResponse;
//import io.fathom.cloud.protobuf.CloudModel.ImageData;
//import io.fathom.cloud.protobuf.CloudModel.ImageLocation;
//import io.fathom.cloud.protobuf.CloudModel.ImageState;
//import io.fathom.cloud.storage.Clock;
//import com.fathomdb.utils.Hex;
//import com.google.common.collect.Lists;
//import com.google.inject.persist.Transactional;
//import com.google.protobuf.ByteString;
//
//@Path("/openstack/images/v2/images")
//@Transactional
//public class ImagesV2Endpoint extends ImageResourceBase {
//	private static final Logger log = LoggerFactory
//			.getLogger(ImagesV2Endpoint.class);
//
//	static DateTimeFormatter DATE_FORMAT = ISODateTimeFormat.dateTime();
//
//	@Inject
//	ImageService imageService;
//
//	@Inject
//	ImageDataService imageDataService;
//
//	@PathParam("imageId")
//	String imageId;
//
//	private ImageData findImage() throws CloudException {
//		return imageService.findImage(getProject(), Long.valueOf(imageId));
//	}
//
//	@PUT
//	@Path("{imageId}/file")
//	public Response updateContent(File file) throws CloudException, IOException {
//		ImageData image = findImage();
//		if (image == null) {
//			throw new WebApplicationException(Status.NOT_FOUND);
//		}
//
//		BlobData data = BlobData.build(file);
//
//		String stored = imageDataService.storeImageFile(image, data);
//
//		ImageLocation.Builder b = ImageLocation.newBuilder();
//		b.setStored(stored);
//
//		imageService.setImageLocation(getProject(), image.getId(), data.size(),
//				data.getHash(), b.build());
//
//		return Response.status(Status.NO_CONTENT).build();
//	}
//
//	@GET
//	@Path("{id}/file")
//	public Response getContent() throws CloudException, IOException {
//		ImageData image = findImage();
//		if (image == null) {
//			throw new WebApplicationException(Status.NOT_FOUND);
//		}
//
//		ImageLocation imageLocation = image.getLocation();
//		if (imageLocation.hasStored()) {
//			String cookie = imageLocation.getStored();
//
//			BlobData blob = imageDataService.getImageFile(cookie);
//
//			ResponseBuilder response = Response.ok().entity(blob.asEntity());
//
//			ByteString md5 = blob.getHash();
//			response.header("Content-MD5", Hex.toHex(md5.toByteArray()));
//			return response.build();
//		}
//
//		return Response.status(Status.NO_CONTENT).build();
//	}
//
//	@POST
//	@Consumes({ JSON })
//	@Produces({ JSON })
//	public Image createImage(Image image) throws CloudException {
//		ImageData.Builder b = ImageData.newBuilder();
//		b.setName(image.name);
//		b.setImageState(ImageState.QUEUED);
//
//		if (image.tags != null) {
//			b.addAllTag(image.tags);
//		}
//
//		long t = Clock.getTimestamp();
//		b.setCreatedAt(t);
//		b.setUpdatedAt(t);
//
//		b.setIsPublic(image.isPublic);
//
//		b.setOwnerProject(getProject().getId());
//
//		b.setDiskFormat(image.diskFormat);
//		b.setContainerFormat(image.containerFormat);
//
//		ImageData created = imageService.createImage(b);
//
//		return toModel(created);
//	}
//
//	@GET
//	@Path("detail")
//	@Produces({ JSON })
//	public ImageListResponse doImageDetailsGet() throws CloudException {
//		ImageListResponse response = new ImageListResponse();
//
//		response.images = Lists.newArrayList();
//
//		ImageFilter filter = new ImageFilter();
//		filter.name = httpRequest.getParameter("name");
//
//		for (ImageData data : imageService.listImages(getProject(), filter)) {
//			response.images.add(toModel(data));
//		}
//		return response;
//	}
// }