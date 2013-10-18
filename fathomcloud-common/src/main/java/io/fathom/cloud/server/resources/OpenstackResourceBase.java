package io.fathom.cloud.server.resources;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.io.Asn;
import io.fathom.cloud.server.auth.Auth;
import io.fathom.cloud.server.auth.AuthProvider;
import io.fathom.cloud.server.model.Project;

import java.io.IOException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;

import javax.inject.Inject;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.io.BaseEncoding;

public class OpenstackResourceBase extends FathomCloudResourceBase {
    private static final Logger log = LoggerFactory.getLogger(OpenstackResourceBase.class);

    // protected static final String AUTH_HEADER = "X-Auth-Token";

    @Context
    protected HttpHeaders httpHeaders;

    // @Inject
    // protected TokenService tokenService;
    //
    @Inject
    protected AuthProvider authProvider;

    private Optional<Auth> auth;

    protected Auth findAuth() {
        if (auth == null) {
            auth = Optional.fromNullable(authProvider.get());
        }
        return auth.orNull();
    }

    public boolean isAuthenticated() {
        Auth auth = findAuth();
        if (auth == null) {
            return false;
        }
        assert auth.getUser() != null;
        return true;
    }

    protected Project findProject(long projectId) throws CloudException {
        Auth auth = findAuth();
        if (auth == null) {
            return null;
        }

        if (!auth.checkProject(projectId)) {
            return null;
        }

        Project project = new Project(projectId);
        return project;
    }

    protected Project findProject(String projectName) throws CloudException {
        if (Strings.isNullOrEmpty(projectName)) {
            return null;
        }
        long projectId = Long.valueOf(projectName);
        return findProject(projectId);
    }

    // // protected ProjectData findProject(UserData userData, long projectId)
    // // throws CloudException {
    // // ProjectData project = null;
    // //
    // // List<Long> projectIds = userData.getProjectIdsList();
    // // if (projectIds.contains(projectId)) {
    // // project = authStore.getProjects().find(projectId);
    // // }
    // //
    // // return project;
    // // }

    // protected Project findProjectFromToken() throws CloudException {
    // Token token = findTokenInfo();
    // if (token == null) {
    // return null;
    // }
    //
    // Project project = findProject(token);
    // return project;
    // }
    //

    // protected boolean hasToken() {
    // List<String> values = httpHeaders.getRequestHeader(AUTH_HEADER);
    // if (values == null || values.isEmpty()) {
    // return false;
    // }
    // return true;
    // }

    // private User user;
    //
    // protected User findUserFromToken() throws CloudException {
    // if (user == null) {
    // Token tokenInfo = findTokenInfo();
    // user = findUserFromToken(tokenInfo);
    // }
    // return user;
    // }
    //

    // protected Project findProject(Token tokenInfo) throws CloudException {
    // if (tokenInfo == null) {
    // return null;
    // }
    //
    // if (token.hasExpired()) {
    // // This is treated the same as an invalid token
    // return null;
    // }
    //
    // if (!tokenInfo.hasProjectId()) {
    // return null;
    // }
    //
    // long projectId = tokenInfo.getProjectId();
    // return new Project(projectId);
    // // authStore.getUsers().find(userId);
    // }
    //
    // // protected User getUser() throws CloudException {
    // // User user = findUser();
    // // if (user == null) {
    // // throw new WebApplicationException(Status.UNAUTHORIZED);
    // // }
    // // return user;
    // // }

    protected ClientCertificate getClientCertificate() {
        // X509Certificate[] certChain = (X509Certificate[]) httpRequest
        // .getAttribute("javax.servlet.request.X509Certificate");
        // if (certChain != null && certChain.length != 0) {
        // PublicKey publicKey = certificateChain[0].getPublicKey();
        // return null;
        // }

        // String clientSha1 = httpRequest.getHeader("X-SSL-Client-Key-SHA1");
        // if (!Strings.isNullOrEmpty(clientSha1)) {
        // // TODO: Do we always want to enable this??
        // ByteString sha1 =
        // ByteString.copyFrom(BaseEncoding.base16().decode(clientSha1));
        // return new ClientCertificate(sha1);
        // }

        String clientKey = httpRequest.getHeader("X-SSL-Client-Key");
        if (!Strings.isNullOrEmpty(clientKey)) {
            // TODO: Do we always want to enable this??
            byte[] bytes = BaseEncoding.base16().decode(clientKey);

            // RSAPublicKey rsaPublicKey = RSAPublicKey.getInstance(bytes);

            PublicKey publicKey;
            try {
                publicKey = Asn.readRsaPublicKey(bytes);
                // KeyFactory.getInstance("RSA").generatePublic(new
                // X509EncodedKeySpec(bytes));
            } catch (InvalidKeySpecException e) {
                log.debug("Key was: {}", clientKey);
                throw new IllegalArgumentException("Invalid key");
            } catch (IOException e) {
                log.debug("Key was: {}", clientKey);
                throw new IllegalArgumentException("Error loading key");
            }
            return new ClientCertificate(publicKey);
        }

        // X509Certificate[] certificateChain = getCertificateChain();
        // if (certificateChain == null || certificateChain.length == 0) {
        // // log.info("Headers: X-SSL " + httpRequest.getHeader("X-SSL"));
        // // log.info("Headers: X-SSL-Client-SHA1 " +
        // httpRequest.getHeader("X-SSL-Client-SHA1"));
        // throw new
        // IllegalArgumentException("Client certificate not provided");
        // }

        return null;
    }
}
