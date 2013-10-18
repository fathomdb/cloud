package io.fathom.auto.openstack.secrets;

import io.fathom.auto.config.SecretKeys;
import io.fathom.cloud.openstack.client.OpenstackClient;
import io.fathom.cloud.openstack.client.RestClientException;
import io.fathom.cloud.openstack.client.keystore.OpenstackKeystoreClient;
import io.fathom.cloud.openstack.client.keystore.Secret;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;

public class OpenstackSecretKeys implements SecretKeys {
    private static final Logger log = LoggerFactory.getLogger(OpenstackSecretKeys.class);

    private final OpenstackClient client;

    Map<String, Secret> secrets;

    public OpenstackSecretKeys(OpenstackClient client) {
        this.client = client;
    }

    private Map<String, Secret> collectKeys() throws RestClientException {
        OpenstackKeystoreClient keystore = client.getKeystore();

        Map<String, Secret> secrets = Maps.newHashMap();

        for (Secret secret : keystore.listSecrets()) {
            log.debug("Found secret: " + secret);

            String subject = secret.subject;
            if (Strings.isNullOrEmpty(subject)) {
                continue;
            }

            secrets.put(subject, secret);
        }

        return secrets;
    }

    // public File buildKey(Secret secret) throws IOException {
    // String filename = secret.id + "_" + secret.version;
    // File file = new File(keyPath, filename);
    // if (!file.exists()) {
    // String secretData;
    // try {
    // secretData = encodePem(secret);
    // } catch (RestClientException e) {
    // throw new IOException("Error reading secret", e);
    // }
    // Files.write(secretData, file, Charsets.UTF_8);
    // }
    // return file;
    // }

    String readSecret(Secret secret) throws RestClientException {
        OpenstackKeystoreClient keystore = client.getKeystore();

        byte[] certificate = keystore.getSecret(secret.id, Secret.CERTIFICATE);
        byte[] privateKey = keystore.getSecret(secret.id, Secret.PRIVATE_KEY);

        // We assume PEM format
        String pem = new String(certificate, Charsets.UTF_8) + "\n" + new String(privateKey, Charsets.UTF_8);
        return pem;
    }

    @Override
    public SecretInfo findSecret(String host) {
        Secret secret = secrets.get(host);
        if (secret != null) {
            return new OpenstackSecretInfo(this, secret);
        }

        int firstDot = host.indexOf('.');
        if (firstDot != -1) {
            String wildcard = "*" + host.substring(firstDot);
            secret = secrets.get(wildcard);
            if (secret != null) {
                return new OpenstackSecretInfo(this, secret);
            }
        }

        return null;
    }

    @Override
    public void refresh() throws IOException {
        try {
            this.secrets = collectKeys();
        } catch (RestClientException e) {
            throw new IOException("Error refreshing secrets", e);
        }
    }

}
