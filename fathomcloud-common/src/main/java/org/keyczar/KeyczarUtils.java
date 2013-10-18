package org.keyczar;

import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.interfaces.PBEKey;
import javax.crypto.spec.PBEKeySpec;

import org.keyczar.enums.KeyPurpose;
import org.keyczar.enums.KeyStatus;
import org.keyczar.exceptions.KeyczarException;
import org.keyczar.interfaces.KeyczarReader;
import org.keyczar.util.Base64Coder;
import org.keyczar.util.Util;

import com.google.common.base.Charsets;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

public class KeyczarUtils {
    public static AesKey unpack(byte[] data) throws KeyczarException {
        AesKey aesKey = AesKey.fromPackedKey(data);
        return aesKey;
    }

    public static KeyczarReader asReader(AesKey aesKey) {
        ImportedKeyReader importedKeyReader = new ImportedKeyReader(aesKey);
        return importedKeyReader;
    }

    public static byte[] pack(AesKey key) {
        return key.getEncoded();
    }

    @Deprecated
    public static SecretKey getKey(AesKey key) {
        return key.getJceKey();
    }

    public static byte[] generate(KeyMetadata kmd) throws KeyczarException {
        KeyczarKey key = createKey(kmd);

        byte[] packed = KeyczarUtils.pack((AesKey) key);
        return packed;
    }

    public static KeyczarKey createKey(KeyMetadata kmd) {
        try {
            MemoryKeyczarStore store = new MemoryKeyczarStore();
            GenericKeyczar keyczar = GenericKeyczar.create(store, kmd);

            keyczar.addVersion(KeyStatus.PRIMARY);

            KeyczarKey key = keyczar.getPrimaryKey();
            return key;
        } catch (KeyczarException e) {
            throw new IllegalStateException("Error generating key", e);
        }
    }

    public static AesKey generateSymmetricKey() {
        KeyMetadata kmd = new KeyMetadata("key", KeyPurpose.DECRYPT_AND_ENCRYPT, DefaultKeyType.AES);
        return (AesKey) createKey(kmd);
    }

    public static byte[] encrypt(AesKey key, byte[] plaintext) {
        try {
            Crypter crypter = buildCrypter(key);
            return crypter.encrypt(plaintext);
        } catch (KeyczarException e) {
            throw new IllegalStateException("Error encrypting data", e);
        }
    }

    public static Crypter buildCrypter(AesKey key) {
        try {
            KeyczarReader importedKeyReader = asReader(key);
            Crypter crypter = new Crypter(importedKeyReader);
            return crypter;
        } catch (KeyczarException e) {
            throw new IllegalStateException("Error wrapping key", e);
        }
    }

    public static AesKey deriveKey(int iterations, byte[] salt, String password) {
        int keyLength = 256;
        PBEKeySpec pbeKeySpec = new PBEKeySpec(password.toCharArray(), salt, iterations, keyLength);
        SecretKeyFactory factory;
        try {
            factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to get PBKDF2 provider", e);
        }
        PBEKey pbeKey;
        try {
            pbeKey = (PBEKey) factory.generateSecret(pbeKeySpec);
        } catch (InvalidKeySpecException e) {
            throw new IllegalStateException("Error generating secret", e);
        }

        byte[] aesBytes = pbeKey.getEncoded();
        if (aesBytes.length != (256 / 8)) {
            throw new IllegalStateException();
        }

        try {
            HmacKey hmacKey = deriveHmac(aesBytes, salt, password);
            return new AesKey(aesBytes, hmacKey);
        } catch (KeyczarException e) {
            throw new IllegalStateException("Error building key", e);
        }
    }

    public static HmacKey deriveHmac(byte[] aesBytes, byte[] salt, String password) {
        byte[] hmacBytes;
        {
            // We need something that can be consistent
            // Some people say it's OK to just use the hash of the key,
            // but obviously there's no proof of that. We add a little
            // complexity
            // to be safer.
            Hasher hasher = Hashing.sha256().newHasher();
            byte[] passwordBytes = password.getBytes(Charsets.UTF_8);
            hasher.putBytes(passwordBytes);
            hasher.putBytes(aesBytes);
            hasher.putBytes(passwordBytes);
            hasher.putBytes(salt);
            hasher.putBytes(passwordBytes);
            hmacBytes = hasher.hash().asBytes();
        }
        if (hmacBytes.length != (256 / 8)) {
            throw new IllegalStateException();
        }
        try {
            HmacKey hmacKey = new HmacKey(hmacBytes);
            return hmacKey;
        } catch (KeyczarException e) {
            throw new IllegalStateException("Error building key", e);
        }
    }

    public static byte[] decrypt(AesKey newKey, byte[] ciphertext) throws KeyczarException {
        Crypter crypter = buildCrypter(newKey);
        return crypter.decrypt(ciphertext);
    }

    public static RsaPrivateKey readRsaPrivateKey(String data) throws KeyczarException {
        return RsaPrivateKey.read(data);
    }

    public static RsaPublicKey readRsaPublicKey(String data) throws KeyczarException {
        return RsaPublicKey.read(data);
    }

    public static byte[] generateSecureRandom(int len) {
        return org.keyczar.util.Util.rand(len);
    }

    public static KeyczarPublicKey getPublicKey(KeyczarKey keypair) {
        return ((KeyczarPrivateKey) keypair).getPublic();
    }

    public static RsaPrivateKey getPrivateKey(KeyczarKey keypair) {
        return ((RsaPrivateKey) keypair);
    }

    public static PublicKey getJce(KeyczarPublicKey keyczarKey) {
        return (PublicKey) keyczarKey.getJceKey();
    }

    public static PrivateKey getJce(RsaPrivateKey keyczarPrivateKey) {
        return keyczarPrivateKey.getJceKey();
    }

    private static final String PEM_FOOTER_BEGIN = "-----END ";
    private static final String PEM_LINE_ENDING = "-----\n";
    private static final String PEM_HEADER_BEGIN = "-----BEGIN ";

    /**
     * Because sometimes we don't want a password...
     */
    public static String toPem(RsaPrivateKey keyczarPrivateKey) {
        RSAPrivateCrtKey jceKey = keyczarPrivateKey.getJceKey();
        byte[] keyData = jceKey.getEncoded();

        String pemType = jceKey.getAlgorithm() + " PRIVATE KEY";
        String base64Key = Base64Coder.encodeMime(keyData, true);
        StringBuffer result = new StringBuffer();
        result.append(PEM_HEADER_BEGIN);
        result.append(pemType);
        result.append(PEM_LINE_ENDING);
        for (String line : Util.split(base64Key, 64)) {
            result.append(line);
            result.append('\n');
        }
        result.append(PEM_FOOTER_BEGIN);
        result.append(pemType);
        result.append(PEM_LINE_ENDING);

        return result.toString();
    }

}
