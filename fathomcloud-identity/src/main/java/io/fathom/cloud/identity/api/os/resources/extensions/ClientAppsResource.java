package io.fathom.cloud.identity.api.os.resources.extensions;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.server.auth.Auth;
import io.fathom.cloud.server.model.Project;
import io.fathom.cloud.services.Attachments;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

@Path("/openstack/identity/extensions/client")
public class ClientAppsResource extends AttachmentsResourceBase {
    private static final Logger log = LoggerFactory.getLogger(ClientAppsResource.class);

    @Inject
    Attachments attachments;

    @POST
    @Produces(JSON)
    public ClientApp createClientApp(ClientApp req) throws CloudException {
        if (Strings.isNullOrEmpty(req.name)) {
            throw new IllegalArgumentException();
        }
        if (Strings.isNullOrEmpty(req.secret)) {
            throw new IllegalArgumentException();
        }

        Auth auth = getAuth();

        Project project = auth.getProject();
        if (project == null) {
            throw new IllegalArgumentException();
        }

        Attachments.ClientApp app = attachments.createClientApp(auth, project, req.name, req.secret);

        ClientApp ret = new ClientApp();
        ret.name = app.getAppName();
        ret.id = app.getAppId();
        return ret;
    }

}