package io.fathom.cloud.secrets.api.os.resources;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.protobuf.SecretsModel.SecretRecordData;
import io.fathom.cloud.secrets.api.os.model.Secret;
import io.fathom.cloud.secrets.api.os.model.SecretList;
import io.fathom.cloud.secrets.services.SecretImpl;
import io.fathom.cloud.server.auth.Auth;
import io.fathom.cloud.server.model.Project;
import io.fathom.cloud.services.SecretService;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

@Path("/openstack/keystore/{project}/secrets")
public class SecretResource extends SecretResourceBase {
    private static final Logger log = LoggerFactory.getLogger(SecretResource.class);

    @Inject
    SecretService secretsService;

    @GET
    @Path("{id}")
    public Secret findSecret(@PathParam("id") long id) throws CloudException {
        Auth auth = getAuth();
        Project project = getProject();

        SecretService.Secret secret = secretsService.find(auth, project, id);
        notFoundIfNull(secret);

        return toModel(getData(secret));
    }

    @GET
    @Path("{id}/{key}")
    public Response getSecret(@PathParam("id") long id, @PathParam("key") String key) throws CloudException {
        Auth auth = getAuth();
        Project project = getProject();

        SecretService.Secret secret = secretsService.find(auth, project, id);
        notFoundIfNull(secret);

        SecretService.SecretItem item = secret.find(key);
        notFoundIfNull(item);

        byte[] data = item.getBytes();
        return Response.ok(data).build();
    }

    @GET
    public SecretList listSecrets() throws CloudException {
        Auth auth = getAuth();
        Project project = getProject();

        SecretList ret = new SecretList();
        ret.secrets = Lists.newArrayList();

        for (SecretService.Secret secret : secretsService.list(auth, project)) {
            ret.secrets.add(toModel(getData(secret)));
        }

        return ret;
    }

    private SecretRecordData getData(SecretService.Secret secret) {
        return ((SecretImpl) secret).getData();
    }

    private Secret toModel(SecretRecordData data) {
        Secret model = new Secret();
        model.id = Long.toString(data.getId());

        model.algorithm = data.getAlgorithm();

        if (data.hasKeySize()) {
            model.bit_length = data.getKeySize();
        }

        model.subject = data.getSubject();
        model.name = data.getName();

        return model;
    }

}