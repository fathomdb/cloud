package io.fathom.cloud.identity.api.os.resources.extensions;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.identity.api.os.resources.IdentityResourceBase;
import io.fathom.cloud.identity.api.os.resources.RolesResource;
import io.fathom.cloud.identity.services.IdentityService;
import io.fathom.cloud.identity.services.IdentityService.UserCreationData;
import io.fathom.cloud.openstack.client.identity.model.RegisterRequest;
import io.fathom.cloud.openstack.client.identity.model.RegisterResponse;
import io.fathom.cloud.protobuf.IdentityModel.DomainData;
import io.fathom.cloud.protobuf.IdentityModel.UserData;
import io.fathom.cloud.server.resources.ClientCertificate;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.io.BaseEncoding;
import com.google.protobuf.ByteString;

@Path("/openstack/identity/extensions/register")
public class RegisterResource extends IdentityResourceBase {
    private static final Logger log = LoggerFactory.getLogger(RolesResource.class);

    @Inject
    IdentityService identityService;

    @POST
    public RegisterResponse register(RegisterRequest request) throws CloudException {
        String email = request.email;
        if (Strings.isNullOrEmpty(email)) {
            throw new IllegalArgumentException();
        }
        email = email.trim();

        ClientCertificate clientCertificate = getClientCertificate();
        if (clientCertificate == null) {
            throw new IllegalArgumentException("Client certificate not provided");
        }

        if (request.challengeResponse == null || Strings.isNullOrEmpty(request.challengeResponse.response)) {
            ByteString challenge = loginService.createRegistrationChallenge(clientCertificate);

            RegisterResponse response = new RegisterResponse();
            response.challenge = BaseEncoding.base64().encode(challenge.toByteArray());
            return response;
        }

        DomainData domain = identityService.getDefaultDomain();

        UserData.Builder b = UserData.newBuilder();

        // We allow multiple systems to share an email address
        // so we use the public key hash as our unique id
        {
            ByteString publicKeySha1 = clientCertificate.getPublicKeySha1();
            String hex = BaseEncoding.base16().encode(publicKeySha1.toByteArray());
            b.setName("__pubkey__" + hex);
        }

        b.setDomainId(domain.getId());

        b.setEnabled(true);

        b.setEmail(request.email);

        String password = null;
        UserCreationData userCreationData = new UserCreationData(domain, b, password);
        userCreationData.publicKeySha1 = clientCertificate.getPublicKeySha1();
        userCreationData.publicKeyChallengeRequest = fromBase64(request.challengeResponse.challenge);
        userCreationData.publicKeyChallengeResponse = fromBase64(request.challengeResponse.response);

        UserData user = identityService.createUser(userCreationData);

        RegisterResponse response = new RegisterResponse();
        response.userId = "" + user.getId();
        return response;
    }

    private static ByteString fromBase64(String s) {
        return ByteString.copyFrom(BaseEncoding.base64().decode(s));
    }
}
