package io.fathom.cloud.secrets.services;

import io.fathom.cloud.protobuf.SecretsModel.SecretRecordData;
import io.fathom.cloud.protobuf.SecretsModel.SecretRecordItemData;
import io.fathom.cloud.server.model.Project;
import io.fathom.cloud.services.SecretService.Secret;
import io.fathom.cloud.services.SecretService.SecretInfo;
import io.fathom.cloud.services.SecretService.SecretItem;

import org.keyczar.Crypter;

public class SecretImpl implements Secret {
    final Project project;
    final SecretRecordData data;
    final Crypter crypter;

    public SecretImpl(Project project, SecretRecordData data, Crypter crypter) {
        this.project = project;
        this.data = data;
        this.crypter = crypter;
    }

    public SecretRecordData getData() {
        return data;
    }

    @Override
    public SecretItem find(String key) {
        for (SecretRecordItemData item : data.getItemList()) {
            if (key.equals(item.getKey())) {
                return new SecretItemImpl(this, item);
            }
        }
        return null;
    }

    Crypter getCrypter() {
        return crypter;
    }

    public Project getProject() {
        return project;
    }

    @Override
    public SecretInfo getSecretInfo() {
        SecretInfo info = new SecretInfo();

        info.algorithm = data.getAlgorithm();
        info.keySize = data.getKeySize();
        info.name = data.getName();
        info.subject = data.getSubject();

        return info;
    }

    @Override
    public long getId() {
        return data.getId();
    }

}
