package io.fathom.cloud.openstack.client.identity;

import io.fathom.cloud.openstack.client.identity.model.V2AuthRequest;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.security.auth.x500.X500Principal;

import com.fathomdb.crypto.CertificateAndKey;
import com.fathomdb.crypto.SimpleCertificateAndKey;
import com.fathomdb.crypto.bouncycastle.SimpleCertificateAuthority;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.io.BaseEncoding;
import com.google.common.primitives.Bytes;

public class ChallengeResponses {

    private static final byte[] PREFIX_V1_BYTES = "openstack-rsa-v1".getBytes(Charsets.UTF_8);

    public static boolean hasPrefix(byte[] data) {
        if (data.length < PREFIX_V1_BYTES.length) {
            return false;
        }
        byte[] head = new byte[PREFIX_V1_BYTES.length];
        System.arraycopy(data, 0, head, 0, PREFIX_V1_BYTES.length);
        return Arrays.equals(head, PREFIX_V1_BYTES);
    }

    public static byte[] getPayload(byte[] data) {
        if (!hasPrefix(data)) {
            throw new IllegalArgumentException();
        }
        byte[] tail = new byte[data.length - PREFIX_V1_BYTES.length];
        System.arraycopy(data, PREFIX_V1_BYTES.length, tail, 0, tail.length);
        return tail;
    }

    public static byte[] addHeader(byte[] ciphertext) {
        return Bytes.concat(PREFIX_V1_BYTES, ciphertext);
    }

    public static V2AuthRequest.ChallengeResponse respondToAuthChallenge(CertificateAndKey certificateAndKey,
            String challenge) {
        if (Strings.isNullOrEmpty(challenge)) {
            throw new IllegalStateException("Did not receieve challenge when authenticating");
        }

        byte[] challengeBytes = BaseEncoding.base64().decode(challenge);

        // The prefix acts as a version
        if (!hasPrefix(challengeBytes)) {
            throw new IllegalStateException("Challenge was not in a recognized format");
        }

        byte[] ciphertext = getPayload(challengeBytes);
        byte[] plaintext = decrypt(certificateAndKey.getPrivateKey(), ciphertext);

        // The prefix must also be in the plaintext
        if (!hasPrefix(plaintext)) {
            throw new IllegalStateException("Challenge was not valid (bad decrypted result)");
        }

        V2AuthRequest.ChallengeResponse challengeResponse = new V2AuthRequest.ChallengeResponse();
        challengeResponse.challenge = challenge;
        challengeResponse.response = BaseEncoding.base64().encode(plaintext);

        return challengeResponse;
    }

    public static V2AuthRequest.ChallengeResponse respondToRegistrationChallenge(CertificateAndKey certificateAndKey,
            String challenge) {
        if (Strings.isNullOrEmpty(challenge)) {
            throw new IllegalStateException("Did not receieve challenge when authenticating");
        }

        byte[] challengeBytes = BaseEncoding.base64().decode(challenge);

        // The prefix acts as a version
        if (!hasPrefix(challengeBytes)) {
            throw new IllegalStateException("Challenge was not in a recognized format");
        }

        // The payload is no longer encrypted, because the server doesn't get
        // the public key behind a firewall
        byte[] payload = getPayload(challengeBytes);

        // The prefix must also be in the payload
        if (!hasPrefix(payload)) {
            throw new IllegalStateException("Challenge was not valid");
        }

        byte[] plaintext = encrypt(certificateAndKey.getPublicKey(), payload);

        V2AuthRequest.ChallengeResponse challengeResponse = new V2AuthRequest.ChallengeResponse();
        challengeResponse.challenge = challenge;
        challengeResponse.response = BaseEncoding.base64().encode(plaintext);

        return challengeResponse;
    }

    public static byte[] encrypt(Key key, byte[] plaintext) {
        Cipher cipher = getCipher(key);

        try {
            cipher.init(Cipher.ENCRYPT_MODE, key);
        } catch (InvalidKeyException e) {
            throw new IllegalArgumentException("Invalid key", e);
        }

        byte[] ciphertext;
        try {
            ciphertext = cipher.doFinal(plaintext);
        } catch (IllegalBlockSizeException e) {
            throw new IllegalArgumentException("Error in encryption", e);
        } catch (BadPaddingException e) {
            throw new IllegalArgumentException("Error in encryption", e);
        }
        return ciphertext;
    }

    private static Cipher getCipher(Key key) {
        Cipher cipher;

        String algorithm = "RSA";
        try {
            cipher = Cipher.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Error loading crypto provider", e);
        } catch (NoSuchPaddingException e) {
            throw new IllegalArgumentException("Error loading crypto provider", e);
        }
        return cipher;
    }

    static byte[] decrypt(Key key, byte[] ciphertext) {
        Cipher cipher = getCipher(key);

        try {
            cipher.init(Cipher.DECRYPT_MODE, key);
        } catch (InvalidKeyException e) {
            throw new IllegalArgumentException("Invalid key", e);
        }

        byte[] plaintext;
        try {
            plaintext = cipher.doFinal(ciphertext);
        } catch (IllegalBlockSizeException e) {
            throw new IllegalArgumentException("Error in encryption", e);
        } catch (BadPaddingException e) {
            throw new IllegalArgumentException("Error in encryption", e);
        }
        return plaintext;
    }

    public static CertificateAndKey createSelfSigned(X500Principal principal, KeyPair keypair) {
        X500Principal subject = principal;
        PublicKey subjectPublicKey = keypair.getPublic();
        X500Principal issuer = principal;
        PrivateKey issuerPrivateKey = keypair.getPrivate();
        X509Certificate certificate = SimpleCertificateAuthority.signAsCa(subject, subjectPublicKey, issuer,
                issuerPrivateKey);

        X509Certificate[] certificateChain = new X509Certificate[1];
        certificateChain[0] = certificate;

        return new SimpleCertificateAndKey(certificateChain, keypair.getPrivate());
    }

}
