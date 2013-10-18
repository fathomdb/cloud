package io.fathom.cloud.secrets.commands;

import io.fathom.cloud.commands.AuthenticatedCmdlet;
import io.fathom.cloud.secrets.services.ca.Csr;
import io.fathom.cloud.secrets.services.ca.KeyPairs;
import io.fathom.cloud.secrets.services.ca.SelfSigned;
import io.fathom.cloud.server.auth.Auth;
import io.fathom.cloud.server.model.Project;
import io.fathom.cloud.services.SecretService;
import io.fathom.cloud.services.SecretService.SecretInfo;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.inject.Inject;

import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathomdb.crypto.Certificates;
import com.google.common.base.Charsets;
import com.google.protobuf.Message;

public class SecretKeyCreateCmdlet extends AuthenticatedCmdlet {
    private static final Logger log = LoggerFactory.getLogger(SecretKeyCreateCmdlet.class);

    public SecretKeyCreateCmdlet() {
        super("secret-key-create");
    }

    @Option(name = "-s", usage = "subject", required = true)
    public String subject;

    @Inject
    SecretService secretService;

    @Override
    protected Message run0() throws Exception {
        Auth auth = getAuth();
        Project project = auth.getProject();

        SelfSigned helper = new SelfSigned();

        int keySize = 2048;
        String algorithm = "rsa";

        KeyPair keyPair = KeyPairs.generateKeyPair(algorithm, keySize);
        Csr csr = helper.buildCsr(keyPair, subject);

        List<X509Certificate> certChain = helper.selfSign(csr, keyPair);

        StringBuilder sb = new StringBuilder();
        for (X509Certificate cert : certChain) {
            sb.append(Certificates.toPem(cert));
        }

        SecretInfo secretInfo = new SecretInfo();
        secretInfo.name = "Self signed certificate for " + subject;
        secretInfo.algorithm = algorithm;
        secretInfo.keySize = keySize;
        secretInfo.subject = subject;

        SecretService.Secret secret = secretService.create(auth, project, secretInfo);

        secretService.setSecretItem(auth, secret, "certificate", sb.toString().getBytes(Charsets.UTF_8));

        String encoded = KeyPairs.toPem(keyPair);
        secretService.setSecretItem(auth, secret, "privatekey", encoded.getBytes(Charsets.UTF_8));

        return null;
    }
}
