package io.fathom.cloud.identity;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.identity.api.os.model.v2.V2AuthCredentials;
import io.fathom.cloud.identity.api.os.model.v3.Service;
import io.fathom.cloud.identity.model.AuthenticatedUser;
import io.fathom.cloud.protobuf.CloudCommons.TokenInfo;
import io.fathom.cloud.protobuf.IdentityModel.ProjectData;
import io.fathom.cloud.server.resources.ClientCertificate;

import java.util.List;

import com.fathomdb.TimeSpan;
import com.google.protobuf.ByteString;

public interface LoginService {
    public static final TimeSpan OVER_LIMIT_DELAY = TimeSpan.fromMilliseconds(2000);

    List<Service> buildServiceMap(String baseUrl, ProjectData project);

    AuthenticatedUser authenticate(V2AuthCredentials authRequest, ClientCertificate clientCertificate)
            throws CloudException;

    AuthenticatedUser authenticate(String tokenId) throws CloudException;

    TokenInfo buildTokenInfo(AuthenticatedUser authentication);

    AuthenticatedUser authenticate(TokenInfo tokenInfo) throws CloudException;

    AuthenticatedUser authenticate(Long projectId, String username, String password) throws CloudException;

    ByteString getChallenge(ClientCertificate clientCertificate) throws CloudException;

    AuthenticatedUser authenticate(Long projectId, ClientCertificate clientCertificate, ByteString challenge,
            ByteString response) throws CloudException;

    ByteString createRegistrationChallenge(ClientCertificate clientCertificate) throws CloudException;
}
