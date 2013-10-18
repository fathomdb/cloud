package io.fathom.cloud.identity.secrets;

import io.fathom.cloud.identity.model.AuthenticatedProject;
import io.fathom.cloud.identity.model.AuthenticatedUser;
import io.fathom.cloud.protobuf.CloudCommons.SecretData;

import javax.inject.Singleton;

import org.keyczar.exceptions.KeyczarException;

import com.google.protobuf.ByteString;

@Singleton
public class AppSecrets {

    public void setUserSecret(AuthenticatedUser auth, SecretData.Builder s, byte[] plaintext) throws KeyczarException {
        // We encrypt with the data with the user key, only
        // There's no point encrypting with something derived from the
        // password: if an attacker has the app secret, then they can derive any
        // secret we can derive from it
        byte[] ciphertext = auth.getKeys().getSecretToken().getCrypter().encrypt(plaintext);
        // s.setEncryptedWith(EncryptedWith.SECRET_KEY);
        s.setCiphertext(ByteString.copyFrom(ciphertext));
        s.setVersion(1);
    }

    public void setProjectSecret(AuthenticatedProject project, SecretData.Builder s, byte[] plaintext)
            throws KeyczarException {
        byte[] ciphertext = project.getKeys().getSecretToken().getCrypter().encrypt(plaintext);
        s.setCiphertext(ByteString.copyFrom(ciphertext));
        s.setVersion(1);
    }

    public byte[] decryptUserSecret(AuthenticatedUser auth, SecretData secretData) throws KeyczarException {
        return auth.getKeys().getSecretToken().getCrypter().decrypt(secretData.getCiphertext().toByteArray());
    }

    public byte[] decryptProjectSecret(AuthenticatedProject project, SecretData secretData) throws KeyczarException {
        return project.getKeys().getSecretToken().getCrypter().decrypt(secretData.getCiphertext().toByteArray());
    }
}
