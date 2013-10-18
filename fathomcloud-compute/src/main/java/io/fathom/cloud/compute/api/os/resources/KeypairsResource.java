package io.fathom.cloud.compute.api.os.resources;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.compute.api.os.model.Keypair;
import io.fathom.cloud.compute.api.os.model.ListKeypairsResponse;
import io.fathom.cloud.compute.api.os.model.WrappedKeypair;
import io.fathom.cloud.compute.services.SshKeyPairs;
import io.fathom.cloud.protobuf.CloudModel.KeyPairData;

import java.io.IOException;
import java.security.PublicKey;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.keyczar.KeyczarKey;
import org.keyczar.KeyczarPublicKey;
import org.keyczar.KeyczarUtils;
import org.keyczar.RsaPrivateKey;

import com.fathomdb.crypto.OpenSshUtils;
import com.google.common.collect.Lists;

@Path("/openstack/compute/{project}/os-keypairs")
public class KeypairsResource extends ComputeResourceBase {
    @Inject
    SshKeyPairs keypairs;

    @GET
    public ListKeypairsResponse listKeypairs() throws CloudException {
        ListKeypairsResponse response = new ListKeypairsResponse();
        response.keypairs = Lists.newArrayList();

        for (KeyPairData data : keypairs.list(getProject())) {
            Keypair keypair = toModel(data);
            response.keypairs.add(keypair);
        }

        return response;
    }

    @GET
    @Path("{id}")
    public WrappedKeypair getKeypair(@PathParam("id") String id) throws CloudException {
        KeyPairData keypair = keypairs.findKeyPair(getProject(), id);
        notFoundIfNull(keypair);

        WrappedKeypair response = new WrappedKeypair();
        response.keypair = toModel(keypair);
        return response;
    }

    @DELETE
    @Path("{id}")
    public Response deleteKeypair(@PathParam("id") String id) throws CloudException {
        KeyPairData keypair = keypairs.findKeyPair(getProject(), id);
        notFoundIfNull(keypair);

        keypairs.delete(getProject(), id);
        return Response.accepted().build();
    }

    private Keypair toModel(KeyPairData data) {
        Keypair keypair = new Keypair();
        keypair.name = data.getKey();
        keypair.publicKey = data.getPublicKey();
        keypair.fingerprint = data.getPublicKeyFingerprint();
        return keypair;
    }

    @POST
    public WrappedKeypair createKeypair(WrappedKeypair request) throws CloudException, IOException {
        PublicKey sshPublicKey;
        RsaPrivateKey privateKey = null;

        if (request.keypair.publicKey != null) {
            sshPublicKey = OpenSshUtils.readSshPublicKey(request.keypair.publicKey);
        } else {
            KeyczarKey keypair = keypairs.generateKeypair();

            KeyczarPublicKey keyczarPublicKey = KeyczarUtils.getPublicKey(keypair);
            sshPublicKey = KeyczarUtils.getJce(keyczarPublicKey);

            privateKey = KeyczarUtils.getPrivateKey(keypair);

            // SshKey sshKey = SshKey.generate();
            // publicKey = sshKey.getPublicKey();
            // privateKey = sshKey.getPrivateKey();
            // OpenSshUtils.serialize(sshPublicKey);
        }

        KeyPairData created = keypairs.create(getProject(), request.keypair.name, sshPublicKey);

        WrappedKeypair response = new WrappedKeypair();
        response.keypair = toModel(created);

        if (privateKey != null) {
            response.keypair.privateKey = KeyczarUtils.toPem(privateKey);
        }

        return response;
    }

}
