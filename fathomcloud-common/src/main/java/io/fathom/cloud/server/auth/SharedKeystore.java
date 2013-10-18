package io.fathom.cloud.server.auth;

import org.keyczar.Crypter;
import org.keyczar.Encrypter;
import org.keyczar.KeyMetadata;
import org.keyczar.Signer;

public interface SharedKeystore {
    Signer buildSigner(String key);

    Crypter buildCrypter(String key);

    Encrypter buildEncrypter(String key);

    void ensureCreated(String key, KeyMetadata keyMetadata);
}
