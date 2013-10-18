package io.fathom.cloud.identity.api.os.resources;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.identity.api.os.model.Credential;
import io.fathom.cloud.identity.api.os.model.Credentials;
import io.fathom.cloud.identity.api.os.model.WrappedCredential;
import io.fathom.cloud.identity.state.AuthRepository;
import io.fathom.cloud.protobuf.CloudCommons.TokenInfo;
import io.fathom.cloud.protobuf.IdentityModel.AccessId;
import io.fathom.cloud.protobuf.IdentityModel.CredentialData;
import io.fathom.cloud.protobuf.IdentityModel.UserData;
import io.fathom.cloud.state.DuplicateValueException;

import java.security.SecureRandom;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathomdb.utils.Hex;
import com.google.common.collect.Lists;
import com.google.inject.persist.Transactional;
import com.google.protobuf.ByteString;

@Path("/openstack/identity/v3/users/{userId}/credentials/OS-EC2")
@Transactional
@Produces({ "application/json" })
public class CredentialsResource extends IdentityResourceBase {
    private static final Logger log = LoggerFactory.getLogger(CredentialsResource.class);

    // Should use identity service instead
    @Deprecated
    @Inject
    AuthRepository authRepository;

    @PathParam("userId")
    String userId;

    @GET
    @Produces({ JSON })
    public Credentials listEc2Credentials() throws CloudException {
        UserData user = getUser(Long.valueOf(userId));

        Credentials response = new Credentials();
        response.credentials = Lists.newArrayList();

        log.warn("TODO: EC2 credential enumeration is terrible");

        for (CredentialData data : authRepository.getEc2Credentials().list()) {
            if (data.getUserId() != user.getId()) {
                continue;
            }

            Credential c = toModel(data);
            response.credentials.add(c);
        }

        return response;
    }

    static final SecureRandom secureRandom = new SecureRandom();

    @POST
    @Produces({ JSON })
    public WrappedCredential createEc2Credential() throws CloudException, DuplicateValueException {
        UserData forUser = getUser(Long.valueOf(userId));

        WrappedCredential response = new WrappedCredential();

        TokenInfo tokenInfo = findTokenInfo();
        if (tokenInfo == null) {
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }
        long projectId = tokenInfo.getProjectId();
        if (projectId == 0) {
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }

        String accessId;
        ByteString secret;

        {
            byte[] r = new byte[16];
            synchronized (secureRandom) {
                secureRandom.nextBytes(r);
            }
            secret = ByteString.copyFrom(r);
        }
        {
            AccessId.Builder b = AccessId.newBuilder();
            b.setUserId(forUser.getId());

            byte[] r = new byte[8];
            synchronized (secureRandom) {
                // We don't technically need secure random here, but we want it
                // later!
                secureRandom.nextBytes(r);
            }
            b.setPadding(ByteString.copyFrom(r));

            accessId = Hex.toHex(b.build().toByteArray());
        }

        CredentialData created;

        {
            CredentialData.Builder b = CredentialData.newBuilder();
            b.setUserId(forUser.getId());
            b.setProjectId(projectId);
            b.setKey(accessId);
            b.setDeprecatedSharedSecret(secret);

            created = authRepository.getEc2Credentials().create(b);
        }

        response.credential = toModel(created);

        return response;
    }

    private Credential toModel(CredentialData data) {
        Credential c = new Credential();

        c.secret = Hex.toHex(data.getDeprecatedSharedSecret().toByteArray());
        c.access = data.getKey();
        c.userId = "" + data.getUserId();
        c.tenantId = "" + data.getProjectId();

        return c;
    }

}
