package io.fathom.cloud.identity.services;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.openssl.PEMWriter;

import com.fathomdb.crypto.bouncycastle.BouncyCastleLoader;
import com.fathomdb.io.IoUtils;

public class KeyPairs {
    public static final String DEFAULT_ALGORITHM = "RSA";
    public static final int DEFAULT_KEYSIZE = 2048;

    public static KeyPair deserialize(String keyData) throws IOException {
        PEMReader r = new PEMReader(new StringReader(keyData), null, BouncyCastleLoader.getName());
        try {
            return (KeyPair) r.readObject();
        } finally {
            IoUtils.safeClose(r);
        }
    }

    // public static String serialize(KeyPair keyPair) throws IOException {
    // StringWriter writer = new StringWriter();
    // PEMWriter pemWriter = new PEMWriter(writer,
    // BouncyCastleLoader.getName());
    // try {
    // pemWriter.writeObject(keyPair);
    // pemWriter.flush();
    // return writer.toString();
    // } finally {
    // IoUtils.safeClose(pemWriter);
    // }
    // }

    public static String serializePem(Object data) throws IOException {
        StringWriter writer = new StringWriter();
        try (PEMWriter pemWriter = new PEMWriter(writer)) {
            pemWriter.writeObject(data);
            pemWriter.flush();
            return writer.toString();
        }
    }

    static KeyPair generateKeyPair(String algorithm, Integer keySize) {
        KeyPairGenerator generator;
        try {
            generator = KeyPairGenerator.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Error building keypair generator: " + algorithm, e);
        }
        if (keySize != null) {
            generator.initialize(keySize);
        }
        return generator.generateKeyPair();
    }

    public static KeyPair generateKeyPair() {
        return generateKeyPair(DEFAULT_ALGORITHM, DEFAULT_KEYSIZE);
    }

    private static KeyFactory getKeyFactory() {
        KeyFactory keyFactory;
        try {
            keyFactory = KeyFactory.getInstance(DEFAULT_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Error loading RSA provider", e);
        }
        return keyFactory;
    }

    public static PublicKey deserializePublicKey(byte[] keyData) {
        X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(keyData);

        KeyFactory keyFactory = getKeyFactory();

        PublicKey publicKey;
        try {
            publicKey = keyFactory.generatePublic(pubKeySpec);
        } catch (InvalidKeySpecException e) {
            throw new IllegalArgumentException("Error deserializing public key", e);
        }
        return publicKey;
    }

    public static PrivateKey deserializePrivateKey(byte[] keyData) {
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyData);

        KeyFactory keyFactory = getKeyFactory();

        PrivateKey privateKey;
        try {
            privateKey = keyFactory.generatePrivate(keySpec);
        } catch (InvalidKeySpecException e) {
            throw new IllegalArgumentException("Error deserializing private key", e);
        }
        return privateKey;
    }

    // public static byte[] encrypt(Key key, byte[] plaintext) {
    // return encrypt(getCipher(), key, plaintext);
    // }
    //
    // static Cipher getCipher() {
    // return getCipher(DEFAULT_ALGORITHM);
    // }
    //
    // public static Cipher getCipher(String algorithm) {
    // try {
    // return Cipher.getInstance(algorithm);
    // } catch (NoSuchAlgorithmException e) {
    // throw new IllegalArgumentException("Error loading crypto provider", e);
    // } catch (NoSuchPaddingException e) {
    // throw new IllegalArgumentException("Error loading crypto provider", e);
    // }
    // }
    //
    // private static void initEncrypt(Cipher cipher, Key key) {
    // try {
    // cipher.init(Cipher.ENCRYPT_MODE, key);
    // } catch (InvalidKeyException e) {
    // throw new IllegalArgumentException("Invalid key", e);
    // }
    // }
    //
    // static byte[] encrypt(Cipher cipher, Key key, byte[] plaintext) {
    // initEncrypt(cipher, key);
    // byte[] encryptedBytes;
    // try {
    // encryptedBytes = cipher.doFinal(plaintext);
    // } catch (IllegalBlockSizeException e) {
    // throw new IllegalArgumentException("Error in encryption", e);
    // } catch (BadPaddingException e) {
    // throw new IllegalArgumentException("Error in encryption", e);
    // }
    // return encryptedBytes;
    // }
    //
    // public static byte[] decrypt(PrivateKey key, byte[] cipherText) {
    // return decrypt(getCipher(), key, cipherText);
    //
    // }
    //
    // public static byte[] decrypt(Cipher cipher, Key key, byte[] cipherText) {
    // initDecrypt(cipher, key);
    // byte[] plainText;
    // try {
    // plainText = cipher.doFinal(cipherText);
    // } catch (IllegalBlockSizeException e) {
    // throw new IllegalArgumentException("Error in decryption", e);
    // } catch (BadPaddingException e) {
    // throw new IllegalArgumentException("Error in decryption", e);
    // }
    // return plainText;
    // }
    //
    // private static void initDecrypt(Cipher cipher, Key key) {
    // try {
    // cipher.init(Cipher.DECRYPT_MODE, key);
    // } catch (InvalidKeyException e) {
    // throw new IllegalArgumentException("Invalid key", e);
    // }
    // }

}
