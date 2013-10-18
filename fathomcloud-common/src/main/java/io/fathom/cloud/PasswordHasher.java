package io.fathom.cloud;

import io.fathom.cloud.protobuf.CloudCommons.PasswordHashData;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKeyFactory;
import javax.crypto.interfaces.PBEKey;
import javax.crypto.spec.PBEKeySpec;
import javax.inject.Singleton;

import com.google.protobuf.ByteString;

@Singleton
public class PasswordHasher {
    public static final int DEFAULT_ITERATION_COUNT = 1000;
    public static final int DEFAULT_KEYSIZE = 128;
    public static final int DEFAULT_SALT_LENGTH = 128;

    public static PBEKey doPbkdf2(int iterationCount, byte[] salt, String password, int keyLength) {
        PBEKeySpec pbeKeySpec = new PBEKeySpec(password.toCharArray(), salt, iterationCount, keyLength);
        SecretKeyFactory factory;
        try {
            factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to get PBKDF2 provider", e);
        }
        PBEKey key;
        try {
            key = (PBEKey) factory.generateSecret(pbeKeySpec);
        } catch (InvalidKeySpecException e) {
            throw new IllegalStateException("Error generating secret", e);
        }

        return key;
    }

    public boolean isValid(PasswordHashData passwordHash, String password) {
        byte[] salt = passwordHash.getSalt().toByteArray();
        int rounds = passwordHash.getRounds();
        int keySize = DEFAULT_KEYSIZE;

        PBEKey key = doPbkdf2(rounds, salt, password, keySize);
        byte[] encoded = key.getEncoded();
        byte[] stored = passwordHash.getData().toByteArray();

        return org.keyczar.util.Util.safeArrayEquals(encoded, stored);
    }

    public PasswordHashData hash(String password) {
        PasswordHashData.Builder hasher = PasswordHashData.newBuilder();

        byte[] salt = org.keyczar.util.Util.rand(DEFAULT_SALT_LENGTH / 8);
        int rounds = DEFAULT_ITERATION_COUNT;
        int keySize = DEFAULT_KEYSIZE;

        hasher.setSalt(ByteString.copyFrom(salt));
        hasher.setRounds(rounds);

        PBEKey key = doPbkdf2(rounds, salt, password, keySize);
        byte[] encoded = key.getEncoded();

        hasher.setData(ByteString.copyFrom(encoded));

        return hasher.build();
    }

}
