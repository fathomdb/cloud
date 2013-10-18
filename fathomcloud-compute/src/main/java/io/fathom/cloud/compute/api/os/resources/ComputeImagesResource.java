package io.fathom.cloud.compute.api.os.resources;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.compute.api.os.model.Image;
import io.fathom.cloud.compute.api.os.model.ImageList;
import io.fathom.cloud.compute.api.os.model.WrappedImage;
import io.fathom.cloud.services.ImageService;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

/**
 * This is a passthrough to the image API.
 * 
 * It's sort of deprecated, but still used by clients.
 * 
 */
@Path("/openstack/compute/{project}/images")
public class ComputeImagesResource extends ComputeResourceBase {
    private static final Logger log = LoggerFactory.getLogger(ComputeImagesResource.class);

    @Inject
    ImageService imageService;

    @GET
    public ImageList listImages() throws CloudException {
        return listImages(false);
    }

    @GET
    @Path("detail")
    public ImageList listImageDetails() throws CloudException {
        return listImages(true);
    }

    @GET
    @Path("{id}")
    public WrappedImage getImage(@PathParam("id") long imageId) throws CloudException {
        ImageService.Image image = imageService.findImage(getProject(), imageId);

        notFoundIfNull(image);

        WrappedImage response = new WrappedImage();
        response.image = toModel(image, true);
        return response;
    }

    private ImageList listImages(boolean details) throws CloudException {
        ImageList response = new ImageList();

        response.images = Lists.newArrayList();

        for (ImageService.Image image : imageService.listImages(getProject())) {
            response.images.add(toModel(image, details));
        }

        return response;
    }

    private Image toModel(ImageService.Image image, boolean details) {
        Image model = new Image();
        model.id = "" + image.getId();
        model.name = image.getName();

        if (details) {
            model.status = image.getStatus();

            // "created": "2011-01-01T01:02:03Z",
            // "id": "70a599e0-31e7-49b7-b260-868f441e862b",
            // "links": [
            // {
            // "href":
            // "http://openstack.example.com/v2/openstack/images/70a599e0-31e7-49b7-b260-868f441e862b",
            // "rel": "self"
            // },
            // {
            // "href":
            // "http://openstack.example.com/openstack/images/70a599e0-31e7-49b7-b260-868f441e862b",
            // "rel": "bookmark"
            // },
            // {
            // "href":
            // "http://glance.openstack.example.com/openstack/images/70a599e0-31e7-49b7-b260-868f441e862b",
            // "rel": "alternate",
            // "type": "application/vnd.openstack.image"
            // }
            // ],
            // "metadata": {
            // "architecture": "x86_64",
            // "auto_disk_config": "True",
            // "kernel_id": "nokernel",
            // "ramdisk_id": "nokernel"
            // },
            // "minDisk": 0,
            // "minRam": 0,
            // "name": "fakeimage7",
            // "progress": 100,
            // "status": "ACTIVE",
            // "updated": "2011-01-01T01:02:03Z"
        }

        return model;
    }

}
