package io.fathom.cloud.image.api.os.resources;

import io.fathom.cloud.Clock;
import io.fathom.cloud.CloudException;
import io.fathom.cloud.image.ImageServiceImpl.ImageImpl;
import io.fathom.cloud.image.api.os.model.Image;
import io.fathom.cloud.protobuf.ImageModel.ImageData;
import io.fathom.cloud.server.auth.Auth;
import io.fathom.cloud.server.model.Project;
import io.fathom.cloud.server.resources.OpenstackResourceBase;
import io.fathom.cloud.services.ImageService;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathomdb.utils.Hex;
import com.google.common.collect.Maps;

public class ImageResourceBase extends OpenstackResourceBase {
    private static final Logger log = LoggerFactory.getLogger(ImageResourceBase.class);

    private Project project;

    protected Project getProject() throws CloudException {
        Auth auth = null;
        if (project == null) {
            auth = findAuth();
            if (auth != null) {
                project = auth.getProject();
            }
        }
        if (project == null) {
            log.debug("No project found for auth: {}", auth);
            log.debug("X-Auth-Token: {}", httpRequest.getHeader("X-Auth-Token"));
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }
        return project;
    }

    protected Image toModel(ImageService.Image image) {
        return toModel(((ImageImpl) image).getData());
    }

    protected Image toModel(ImageData data) {
        Image image = new Image();

        image.isPublic = data.getIsPublic();
        if (data.getIsPublic()) {
            image.visibility = "public";
        } else {
            image.visibility = "private";
        }

        image.name = data.getName();
        image.id = "" + data.getId();

        // These are sort of deprecated in V2...
        image.diskFormat = data.getDiskFormat();
        image.containerFormat = data.getContainerFormat();

        image.properties = Maps.newHashMap();
        image.owner = "" + data.getOwnerProject();

        image.isProtected = false;

        String baseUrl = getBaseUrl();

        image.self = baseUrl + "/v1/images/" + data.getId();
        image.file = image.self + "/file";
        image.schema = "/v2/schemas/image";

        if (data.hasImageSize()) {
            image.size = data.getImageSize();
        }

        if (data.hasImageChecksum()) {
            image.checksum = Hex.toHex(data.getImageChecksum().toByteArray());
        }

        switch (data.getImageState()) {
        case ACTIVE:
            image.status = "active";
            break;
        case DELETED:
            image.status = "deleted";
            break;
        case KILLED:
            image.status = "killed";
            break;
        case QUEUED:
            image.status = "queued";
            break;
        case PENDING_DELETE:
            image.status = "pending_delete";
            break;
        case SAVING:
            image.status = "saving";
            break;

        default:
            log.warn("Unknown image state: " + data);
            break;
        }

        if (data.hasCreatedAt()) {
            image.createdAt = Clock.toDate(data.getCreatedAt());
        }
        if (data.hasUpdatedAt()) {
            image.updatedAt = Clock.toDate(data.getUpdatedAt());
        }

        return image;
    }

}
