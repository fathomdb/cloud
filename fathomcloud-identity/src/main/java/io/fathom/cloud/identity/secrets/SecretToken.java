package io.fathom.cloud.identity.secrets;

import javax.crypto.SecretKey;

import org.keyczar.AesKey;
import org.keyczar.Crypter;
import org.keyczar.KeyczarUtils;
import org.keyczar.exceptions.KeyczarException;

import com.fathomdb.crypto.AesCbcCryptoKey;
import com.fathomdb.crypto.CryptoKey;

public class SecretToken {
    public enum SecretTokenType {
        USER_SECRET, CLIENT_APP_SECRET, PROJECT_SECRET
    }

    final SecretTokenType type;
    private final CryptoKey deprecatedKey;
    final AesKey cryptoKey;

    public SecretToken(SecretTokenType type, AesKey cryptoKey, CryptoKey deprecatedKey) {
        this.type = type;
        this.cryptoKey = cryptoKey;
        this.deprecatedKey = deprecatedKey;
    }

    public static SecretToken create(SecretTokenType type) {
        AesKey key = KeyczarUtils.generateSymmetricKey();
        return new SecretToken(type, key, null);
    }

    byte[] encrypt(byte[] plaintext) {
        try {
            Crypter crypter = getCrypter();
            return crypter.encrypt(plaintext);
        } catch (KeyczarException e) {
            throw new IllegalStateException("Error encrypting data", e);
        }
    }

    Crypter getCrypter() {
        // TODO: Cache??
        return KeyczarUtils.buildCrypter(cryptoKey);
    }

    byte[] decrypt(byte[] ciphertext) throws KeyczarException {
        Crypter crypter = getCrypter();
        return crypter.decrypt(ciphertext);
    }

    @Deprecated
    CryptoKey getDeprecatedKey() {
        if (deprecatedKey != null) {
            return deprecatedKey;
        }

        SecretKey key = KeyczarUtils.getKey(cryptoKey);

        return AesCbcCryptoKey.fromJce(key, 128);
    }

}
