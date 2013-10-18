package io.fathom.cloud.secrets.commands;

import io.fathom.cloud.commands.AuthenticatedCmdlet;
import io.fathom.cloud.secrets.services.ca.KeyPairs;
import io.fathom.cloud.server.auth.Auth;
import io.fathom.cloud.server.model.Project;
import io.fathom.cloud.services.SecretService;
import io.fathom.cloud.services.SecretService.Secret;
import io.fathom.cloud.services.SecretService.SecretItem;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.inject.Inject;

import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathomdb.crypto.CertificateAndKey;
import com.fathomdb.crypto.Certificates;
import com.fathomdb.crypto.EncryptionStore;
import com.fathomdb.crypto.SimpleCertificateAndKey;
import com.google.common.base.Charsets;
import com.google.protobuf.Message;

public class SecretKeyActivateCmdlet extends AuthenticatedCmdlet {
    private static final Logger log = LoggerFactory.getLogger(SecretKeyActivateCmdlet.class);

    public SecretKeyActivateCmdlet() {
        super("secret-key-activate");
    }

    @Option(name = "-s", usage = "key subject", required = true)
    public String subject;

    @Option(name = "-alias", usage = "alias to save as", required = false)
    public String alias = "https";

    @Inject
    SecretService secretService;

    @Inject
    EncryptionStore encryptionStore;

    @Override
    protected Message run0() throws Exception {
        Auth auth = getAuth();
        Project project = auth.getProject();

        List<Secret> secrets = secretService.list(auth, project);

        Secret found = null;
        for (Secret secret : secrets) {
            if (subject.equals(secret.getSecretInfo().subject)) {
                if (found != null) {
                    throw new IllegalStateException("Found multiple keys with subject: " + subject);
                }
                found = secret;
            }
        }

        if (found == null) {
            throw new IllegalArgumentException("Key not found with subject: " + subject);
        }

        String certificate = getSecret(found, "certificate");
        List<X509Certificate> certificateChain = Certificates.fromPem(certificate);

        String keypairEncoded = getSecret(found, "privatekey");
        KeyPair keypair = KeyPairs.fromPem(keypairEncoded);

        CertificateAndKey certificateAndKey = new SimpleCertificateAndKey(certificateChain, keypair.getPrivate());
        encryptionStore.setCertificateAndKey(alias, certificateAndKey);

        return null;
    }

    private String getSecret(Secret secret, String key) {
        SecretItem item = secret.find(key);
        if (item == null) {
            throw new IllegalArgumentException("Secret not found: " + key);
        }
        return new String(item.getBytes(), Charsets.UTF_8);
    }
}
