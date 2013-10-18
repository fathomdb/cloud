package io.fathom.cloud.dns.services;

import io.fathom.cloud.lifecycle.LifecycleListener;
import io.fathom.cloud.protobuf.CloudCommons.SecretData;
import io.fathom.cloud.protobuf.DnsModel.BackendData;
import io.fathom.cloud.protobuf.DnsModel.BackendSecretData;
import io.fathom.cloud.server.auth.SharedKeystore;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.keyczar.Crypter;
import org.keyczar.DefaultKeyType;
import org.keyczar.KeyMetadata;
import org.keyczar.enums.KeyPurpose;

import com.google.protobuf.ByteString;

@Singleton
public class DnsSecrets implements LifecycleListener {
    @Inject
    SharedKeystore sharedKeystore;

    public static final String KEY = "dns";

    public BackendSecretData getSecretData(BackendData backendData) {
        try {
            Crypter crypter = sharedKeystore.buildCrypter(KEY);

            byte[] plaintext = crypter.decrypt(backendData.getSecretData().getCiphertext().toByteArray());
            BackendSecretData secretData = BackendSecretData.parseFrom(plaintext);

            return secretData;
        } catch (Exception e) {
            throw new IllegalArgumentException("Error decrypting backend secret", e);
        }
    }

    public SecretData encrypt(BackendSecretData data) {
        try {
            Crypter crypter = sharedKeystore.buildCrypter(KEY);

            byte[] ciphertext = crypter.encrypt(data.toByteArray());
            SecretData.Builder secretData = SecretData.newBuilder();
            secretData.setCiphertext(ByteString.copyFrom(ciphertext));

            return secretData.build();
        } catch (Exception e) {
            throw new IllegalArgumentException("Error encrypting backend secret", e);
        }
    }

    @Override
    public void start() throws Exception {
        String nameFlag = "DNS secret keystore";
        KeyMetadata kmd = new KeyMetadata(nameFlag, KeyPurpose.DECRYPT_AND_ENCRYPT, DefaultKeyType.AES);
        sharedKeystore.ensureCreated(KEY, kmd);
    }

}
