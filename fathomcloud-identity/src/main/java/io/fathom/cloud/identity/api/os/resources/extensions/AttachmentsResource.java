package io.fathom.cloud.identity.api.os.resources.extensions;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.server.auth.Auth;
import io.fathom.cloud.services.Attachments;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.keyczar.exceptions.KeyczarException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

@Path("/openstack/identity/extensions/attachment")
public class AttachmentsResource extends AttachmentsResourceBase {
    private static final Logger log = LoggerFactory.getLogger(AttachmentsResource.class);

    @QueryParam("secret")
    String secret;

    @Inject
    Attachments attachments;

    @PUT
    @Path("user/{userId}/{appId}")
    public Response writeData(@PathParam("userId") long userId, @PathParam("appId") String appId, byte[] payload)
            throws CloudException, KeyczarException {
        // TODO: Allow SSL client certs instead of secret?

        if (Strings.isNullOrEmpty(secret)) {
            throw new IllegalArgumentException();
        }

        // TODO: Cache this computation?
        Attachments.ClientApp app = attachments.findClientAppById(appId, secret);
        if (app == null) {
            log.debug("App not found {}", appId);
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        Auth auth = getAuth();
        if (auth.getUser().getId() != userId) {
            throw new WebApplicationException(Status.FORBIDDEN);
        }

        attachments.setUserSecret(app, auth, payload);

        return Response.noContent().build();
    }

    @GET
    @Path("user/{userId}/{appId}")
    public Response getAttachment(@PathParam("userId") long userId, @PathParam("appId") String appId)
            throws CloudException, KeyczarException {
        Attachments.ClientApp app = attachments.findClientAppById(appId, secret);
        if (app == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        Auth auth = getAuth();
        if (auth.getUser().getId() != userId) {
            throw new WebApplicationException(Status.FORBIDDEN);
        }

        byte[] plaintext = attachments.findUserSecret(app, auth);
        if (plaintext == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        return Response.ok().entity(plaintext).build();
    }

}
