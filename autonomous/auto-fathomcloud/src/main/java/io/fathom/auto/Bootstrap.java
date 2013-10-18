package io.fathom.auto;

import io.fathom.auto.config.BootstrapConfigStoreProvider;
import io.fathom.auto.config.ConfigStore.ConfigStoreProvider;
import io.fathom.cloud.openstack.client.OpenstackClient;
import io.fathom.cloud.openstack.client.RestClientException;
import io.fathom.cloud.openstack.client.identity.CertificateAuthTokenProvider;
import io.fathom.cloud.openstack.client.identity.OpenstackIdentityClient;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.security.KeyPair;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathomdb.crypto.CertificateAndKey;
import com.fathomdb.crypto.bouncycastle.KeyPairs;
import com.fathomdb.properties.PropertyUtils;

public class Bootstrap {
    private static final Logger log = LoggerFactory.getLogger(Bootstrap.class);

    final String serverUri;
    final File privateKeyPath;
    final String email;

    public Bootstrap(File configFile) throws IOException {
        Properties properties = PropertyUtils.loadProperties(configFile);

        this.privateKeyPath = new File("/home/fathomcloud/.ssh/id_rsa");
        this.serverUri = properties.getProperty("server", "https://api-cloud.fathomdb.com/openstack/identity/");
        this.email = properties.getProperty("email");
    }

    public ConfigStoreProvider bootstrap() throws IOException, RestClientException {
        if (this.email == null) {
            log.warn("Email not provided");
            return null;
        }

        if (!privateKeyPath.exists()) {
            log.warn("SSH key file not found: {}", privateKeyPath);
            return null;
        }

        KeyPair keypair = KeyPairs.fromPem(privateKeyPath);

        URI uri = URI.create(serverUri);

        OpenstackIdentityClient identityClient = CertificateAuthTokenProvider.ensureRegistered(keypair, uri, email);

        CertificateAndKey certificateAndKey = CertificateAuthTokenProvider.createSelfSigned(keypair, email);

        String project = identityClient.getUtils().ensureProjectWithPrefix("__federation__");

        CertificateAuthTokenProvider tokenProvider = new CertificateAuthTokenProvider(identityClient, project,
                certificateAndKey);

        OpenstackClient openstackClient = OpenstackClient.build(tokenProvider);

        BootstrapConfigStoreProvider configStoreProvider = new BootstrapConfigStoreProvider(openstackClient);

        return configStoreProvider;
    }
}
