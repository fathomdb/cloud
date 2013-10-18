package io.fathom.cloud.secrets.services;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.keyczar.KeyczarFactory;
import io.fathom.cloud.protobuf.SecretsModel.SecretRecordData;
import io.fathom.cloud.protobuf.SecretsModel.SecretRecordItemData;
import io.fathom.cloud.server.auth.Auth;
import io.fathom.cloud.server.model.Project;
import io.fathom.cloud.services.Attachments;
import io.fathom.cloud.services.AuthService;
import io.fathom.cloud.services.SecretService;
import io.fathom.cloud.services.Attachments.ClientApp;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.keyczar.AesKey;
import org.keyczar.Crypter;
import org.keyczar.KeyczarUtils;
import org.keyczar.exceptions.KeyczarException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathomdb.Configuration;
import com.google.common.collect.Lists;
import com.google.inject.persist.Transactional;
import com.google.protobuf.ByteString;

@Singleton
@Transactional
public class SecretServiceImpl implements SecretService {
    private static final Logger log = LoggerFactory.getLogger(SecretServiceImpl.class);

    @Inject
    KeyczarFactory keyczarFactory;

    @Inject
    Attachments attachments;

    @Inject
    AuthService authService;

    @Inject
    SecretRepository repository;

    @Inject
    Configuration config;

    ClientApp clientApp;

    ClientApp getClientApp() throws CloudException {
        if (clientApp == null) {
            Project project = authService.findSystemProject();
            if (project == null) {
                throw new IllegalStateException("Cannot find system project");
            }

            String appName = config.lookup("secrets.appname", "org.openstack::secrets");

            String appSecret = config.find("secrets.appsecret");
            if (appSecret == null) {
                // We rely on the project secret...
                log.warn("Using default app-secret");
                appSecret = "notasecret";
            }

            this.clientApp = attachments.findClientAppByName(project, appName, appSecret);
            if (clientApp == null) {
                throw new IllegalStateException("Secret store not configured as an application");
            }
        }
        return clientApp;
    }

    @Override
    public List<Secret> list(Auth auth, Project project) throws CloudException {
        Crypter crypter = getSecret(auth, project);

        List<Secret> secrets = Lists.newArrayList();
        for (SecretRecordData data : repository.getSecrets(project).list()) {
            secrets.add(new SecretImpl(project, data, crypter));
        }
        return secrets;
    }

    private Crypter getSecret(Auth auth, Project project) throws CloudException {
        byte[] secret = attachments.findProjectSecret(getClientApp(), auth, project);
        AesKey key;
        if (secret == null) {
            key = KeyczarUtils.generateSymmetricKey();
            secret = KeyczarUtils.pack(key);
            attachments.setProjectSecret(getClientApp(), auth, project, secret);
        } else {
            try {
                key = KeyczarUtils.unpack(secret);
            } catch (KeyczarException e) {
                throw new CloudException("Error unpacking key", e);
            }
        }

        return KeyczarUtils.buildCrypter(key);
    }

    @Override
    public Secret find(Auth auth, Project project, long id) throws CloudException {
        Crypter crypter = getSecret(auth, project);

        SecretRecordData data = repository.getSecrets(project).find(id);
        if (data == null) {
            return null;
        }

        return new SecretImpl(project, data, crypter);
    }

    @Override
    public Secret deleteKey(Auth auth, Project project, long id) throws CloudException {
        Crypter crypter = getSecret(auth, project);

        SecretRecordData data = repository.getSecrets(project).delete(id);
        if (data == null) {
            return null;
        }

        return new SecretImpl(project, data, crypter);
    }

    @Override
    public Secret setSecretItem(Auth auth, Secret secret, String key, byte[] data) throws CloudException {
        SecretImpl secretImpl = (SecretImpl) secret;
        secretImpl = (SecretImpl) find(auth, secretImpl.getProject(), secretImpl.getData().getId());
        SecretRecordData secretData = secretImpl.getData();

        SecretRecordData.Builder b = SecretRecordData.newBuilder(secretData);
        SecretRecordItemData.Builder item = null;
        for (SecretRecordItemData.Builder i : b.getItemBuilderList()) {
            if (i.getKey().equals(key)) {
                item = i;
                break;
            }
        }
        if (item == null) {
            item = b.addItemBuilder();
            item.setKey(key);
        }

        Crypter crypter = secretImpl.getCrypter();
        byte[] ciphertext;
        try {
            ciphertext = crypter.encrypt(data);
        } catch (KeyczarException e) {
            throw new IllegalStateException("Error encrypting secret", e);
        }
        item.setCiphertext(ByteString.copyFrom(ciphertext));

        Project project = secretImpl.getProject();

        secretData = repository.getSecrets(project).update(b);
        return new SecretImpl(project, secretData, crypter);
    }

    @Override
    public Secret create(Auth auth, Project project, SecretInfo secretInfo) throws CloudException {
        Crypter crypter = getSecret(auth, project);

        SecretRecordData.Builder b = SecretRecordData.newBuilder();
        if (secretInfo.name != null) {
            b.setName(secretInfo.name);
        }
        if (secretInfo.algorithm != null) {
            b.setAlgorithm(secretInfo.algorithm);
        }
        if (secretInfo.keySize != 0) {
            b.setKeySize(secretInfo.keySize);
        }
        if (secretInfo.subject != null) {
            b.setSubject(secretInfo.subject);
        }

        SecretRecordData secretData = repository.getSecrets(project).create(b);
        return new SecretImpl(project, secretData, crypter);
    }

}
