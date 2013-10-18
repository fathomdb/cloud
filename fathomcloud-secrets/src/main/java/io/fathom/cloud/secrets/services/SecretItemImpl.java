package io.fathom.cloud.secrets.services;

import io.fathom.cloud.protobuf.SecretsModel.SecretRecordItemData;
import io.fathom.cloud.services.SecretService.SecretItem;

import org.keyczar.Crypter;
import org.keyczar.exceptions.KeyczarException;

public class SecretItemImpl implements SecretItem {

    private final SecretImpl parent;
    private final SecretRecordItemData data;

    public SecretItemImpl(SecretImpl parent, SecretRecordItemData data) {
        this.parent = parent;
        this.data = data;
    }

    @Override
    public byte[] getBytes() {
        Crypter crypter = parent.getCrypter();
        byte[] plaintext;
        try {
            plaintext = crypter.decrypt(data.getCiphertext().toByteArray());
        } catch (KeyczarException e) {
            throw new IllegalStateException("Error decrypting secret", e);
        }
        return plaintext;
    }

}
