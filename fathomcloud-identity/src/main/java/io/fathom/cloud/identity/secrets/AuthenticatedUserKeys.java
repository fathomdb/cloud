package io.fathom.cloud.identity.secrets;

import io.fathom.cloud.protobuf.IdentityModel.UserSecretData;

import org.keyczar.Crypter;
import org.keyczar.KeyczarReaderWrapper;
import org.keyczar.KeyczarUtils;
import org.keyczar.RsaPrivateKey;
import org.keyczar.exceptions.KeyczarException;

public class AuthenticatedUserKeys {
    // private PrivateKey deprecatedPrivateKey = null;
    private RsaPrivateKey privateKey = null;

    final UserWithSecret user;
    final UserSecretData userSecretData;

    public AuthenticatedUserKeys(UserWithSecret user) {
        this.user = user;
        this.userSecretData = user.userSecretData;
    }

    SecretToken getSecretToken() {
        return user.getSecretToken();
    }

    // @Deprecated
    // public PrivateKey getDeprecatedPrivateKey() {
    // if (deprecatedPrivateKey == null) {
    // deprecatedPrivateKey =
    // KeyPairs.deserializePrivateKey(userSecretData.getPrivateKey().getEncoded()
    // .toByteArray());
    // }
    // return deprecatedPrivateKey;
    // }

    Crypter getAsymetricCrypter() {
        try {
            if (privateKey == null) {
                privateKey = KeyczarUtils.readRsaPrivateKey(userSecretData.getPrivateKey().getKeyczar());
            }
            // TODO: Cache crypter?
            return new Crypter(new KeyczarReaderWrapper(privateKey));
        } catch (KeyczarException e) {
            throw new IllegalStateException("Error reading private key", e);
        }
    }
}
