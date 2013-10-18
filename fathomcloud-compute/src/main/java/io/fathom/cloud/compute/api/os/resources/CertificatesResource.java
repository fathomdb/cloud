package io.fathom.cloud.compute.api.os.resources;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.compute.api.os.model.Certificate;
import io.fathom.cloud.compute.api.os.model.Certificates;
import io.fathom.cloud.compute.api.os.model.WrappedCertificate;
import io.fathom.cloud.server.auth.Auth;
import io.fathom.cloud.server.model.User;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.security.auth.x500.X500Principal;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import com.fathomdb.crypto.CertificateAndKey;
import com.fathomdb.crypto.KeyStoreUtils;
import com.fathomdb.utils.Hex;
import com.google.common.collect.Lists;
import com.google.inject.persist.Transactional;

@Path("/openstack/compute/{project}/os-certificates")
@Transactional
public class CertificatesResource extends ComputeResourceBase {

    @GET
    @Produces({ JSON })
    public Certificates list() throws CloudException {
        warnStub();

        Certificates response = new Certificates();
        response.certificates = Lists.newArrayList();

        return response;
    }

    @GET
    @Path("{id}")
    @Produces({ JSON })
    public WrappedCertificate getCertificate(@PathParam("id") String id) throws CloudException {
        // This is entirely wrong
        warnStub();

        Auth auth = getAuth();

        User user = auth.getUser();
        if (user == null) {
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }

        if (!id.equals("root")) {
            throw new IllegalStateException();
        }

        X500Principal subject = new X500Principal("CN=" + "root");
        CertificateAndKey certificateAndKey = createSelfSigned(subject, 2048);

        WrappedCertificate response = new WrappedCertificate();
        response.certificate = new Certificate();
        response.certificate.data = Hex.toHex(certificateAndKey.getPublicKey().getEncoded());
        return response;
    }

    @POST
    @Produces({ JSON })
    public WrappedCertificate create() throws CloudException {
        // This is entirely wrong
        warnStub();

        WrappedCertificate response = new WrappedCertificate();
        response.certificate = new Certificate();

        Auth auth = getAuth();

        User user = auth.getUser();
        if (user == null) {
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }

        X500Principal subject = new X500Principal("CN=" + "user-" + user.getId());

        CertificateAndKey certificateAndKey = createSelfSigned(subject, 2048);

        response.certificate.privateKey = Hex.toHex(certificateAndKey.getPrivateKey().getEncoded());
        response.certificate.data = Hex.toHex(certificateAndKey.getPublicKey().getEncoded());

        return response;
    }

    static CertificateAndKey createSelfSigned(X500Principal principal, int keySize) {
        try {
            String keyAlgorithmName = "RSA";
            String signatureAlgName = "SHA1WithRSA";

            String keyPassword = KeyStoreUtils.DEFAULT_KEYSTORE_SECRET;

            int validityDays = 365 * 10;

            String alias = "self";

            sun.security.x509.X500Name x500Name = new sun.security.x509.X500Name(
                    principal.getName(X500Principal.RFC2253));

            KeyStore keyStore = KeyStoreUtils.createEmpty(KeyStoreUtils.DEFAULT_KEYSTORE_SECRET);
            KeyStoreUtils.createSelfSigned(keyStore, alias, keyPassword, x500Name, validityDays, keyAlgorithmName,
                    keySize, signatureAlgName);

            return KeyStoreUtils.getCertificateAndKey(keyStore, alias, keyPassword);
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("Error creating self-signed certificate", e);
        } catch (IOException e) {
            throw new IllegalArgumentException("Error creating self-signed certificate", e);
        }
    }

}
