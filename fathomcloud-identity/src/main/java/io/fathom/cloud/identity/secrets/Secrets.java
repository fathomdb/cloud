package io.fathom.cloud.identity.secrets;

import io.fathom.cloud.identity.Users;
import io.fathom.cloud.identity.model.AuthenticatedProject;
import io.fathom.cloud.identity.model.AuthenticatedUser;
import io.fathom.cloud.identity.secrets.SecretToken.SecretTokenType;
import io.fathom.cloud.identity.state.AuthRepository;
import io.fathom.cloud.openstack.client.identity.ChallengeResponses;
import io.fathom.cloud.protobuf.CloudCommons.EncryptedWith;
import io.fathom.cloud.protobuf.CloudCommons.SecretData;
import io.fathom.cloud.protobuf.CloudCommons.TokenInfo;
import io.fathom.cloud.protobuf.IdentityModel.ClientAppSecretData;
import io.fathom.cloud.protobuf.IdentityModel.CredentialData;
import io.fathom.cloud.protobuf.IdentityModel.KeyData;
import io.fathom.cloud.protobuf.IdentityModel.ProjectData;
import io.fathom.cloud.protobuf.IdentityModel.ProjectRoles;
import io.fathom.cloud.protobuf.IdentityModel.ProjectRolesSecretData;
import io.fathom.cloud.protobuf.IdentityModel.SecretKeyData;
import io.fathom.cloud.protobuf.IdentityModel.SecretKeyType;
import io.fathom.cloud.protobuf.IdentityModel.SecretStoreData;
import io.fathom.cloud.protobuf.IdentityModel.UserData;
import io.fathom.cloud.protobuf.IdentityModel.UserSecretData;
import io.fathom.cloud.server.auth.SharedKeystore;
import io.fathom.cloud.server.resources.ClientCertificate;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.keyczar.AesKey;
import org.keyczar.Crypter;
import org.keyczar.Encrypter;
import org.keyczar.HmacKey;
import org.keyczar.KeyczarReaderWrapper;
import org.keyczar.KeyczarUtils;
import org.keyczar.RsaPrivateKey;
import org.keyczar.RsaPublicKey;
import org.keyczar.exceptions.KeyczarException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathomdb.crypto.AesCbcCryptoKey;
import com.fathomdb.crypto.CryptoKey;
import com.fathomdb.crypto.FathomdbCrypto;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.InvalidProtocolBufferException;

@Singleton
public class Secrets {
    private static final Logger log = LoggerFactory.getLogger(Secrets.class);

    public static final String KEY_TOKEN_ENCRYPT = "token_encrypt";
    public static final String KEY_FORGOT_PASSWORD_PUBLIC = "forgot_password_public";

    @Inject
    AuthRepository authRepository;

    @Inject
    SharedKeystore keystore;

    @Inject
    Migrations migrations;

    public ByteString buildTokenSecret(AuthenticatedUser user) {
        AesKey userKey = user.getKeys().getSecretToken().cryptoKey;

        AesKey tokenKey = userKey;
        byte[] plaintext = KeyczarUtils.pack(tokenKey);

        // TODO: Key rotation
        byte[] tokenKeySerialized;
        try {
            tokenKeySerialized = getCrypter().encrypt(plaintext);
        } catch (KeyczarException e) {
            throw new IllegalStateException();
        }

        return ByteString.copyFrom(tokenKeySerialized);
    }

    Crypter crypter;

    private synchronized Crypter getCrypter() {
        if (crypter == null) {
            crypter = keystore.buildCrypter(KEY_TOKEN_ENCRYPT);
        }
        return crypter;
    }

    public UserWithSecret getFromToken(UserData user, TokenInfo token) throws KeyczarException {
        byte[] tokenKeySerialized;
        try {
            ByteString tokenSecret = token.getTokenSecret();
            tokenKeySerialized = getCrypter().decrypt(tokenSecret.toByteArray());
        } catch (KeyczarException e) {
            // This should have been validated
            log.warn("Error decrypting user key");
            return null;
        }

        AesKey tokenKey = KeyczarUtils.unpack(tokenKeySerialized);

        // We could have extra layers here, but I don't think they achieve
        // anything
        AesKey userKey = tokenKey;

        SecretToken secretToken = new SecretToken(SecretTokenType.USER_SECRET, userKey, null);
        return checkSecret(user, secretToken);
    }

    public UserWithSecret checkPassword(UserData user, CredentialData credential, String password) {
        // if (user.getName().equals("admin")) {
        // try {
        // user = hotfixDomainAdmin(user);
        // } catch (Exception e) {
        // throw new IllegalStateException("Error migrating user", e);
        // }
        // }

        if (credential.hasDeprecatedPasswordHash()) {
            // TODO: We need to remove these
            log.warn("Credential still has deprecated password hash");
            try {
                user = migrations.migrateUser(credential, user, password);
            } catch (Exception e) {
                throw new IllegalStateException("Error migrating user", e);
            }
        }

        SecretToken secretToken = null;

        try {
            secretToken = getSecretFromPassword(user.getSecretStore(), password);
        } catch (KeyczarException e) {
            log.info("Error while checking password", e);
        }

        if (secretToken == null) {
            return null;
        }

        UserWithSecret ret = checkSecret(user, secretToken);

        if (ret != null) {
            try {
                ret = migrations.migrateUser(ret, password, secretToken);
            } catch (Exception e) {
                throw new IllegalStateException("Error migrating user", e);
            }
        }

        if (ret != null) {
            try {
                ret = migrations.migrateUserAddPasswordRecovery(ret, secretToken);
            } catch (Exception e) {
                throw new IllegalStateException("Error migrating user", e);
            }
        }

        return ret;
    }

    // private UserData hotfixDomainAdmin(UserData user) throws CloudException {
    // if (user.getDomainRolesCount() != 0) {
    // return user;
    // }
    //
    // log.warn("Hotfixing admin user with domain admin role");
    //
    // UserData.Builder b = UserData.newBuilder(user);
    //
    // DomainRoles.Builder rb = b.addDomainRolesBuilder();
    // rb.setDomain(user.getDomainId());
    // rb.addRole(WellKnownRoles.ROLE_ID_ADMIN);
    //
    // user = authRepository.getUsers().update(b);
    //
    // return user;
    // }

    private UserWithSecret checkSecret(UserData user, SecretToken secretToken) {
        SecretData secretData = user.getSecretData();

        if (secretData.hasEncryptedWith() && secretData.getEncryptedWith() != EncryptedWith.SECRET_KEY) {
            throw new IllegalStateException();
        }

        UserSecretData userSecretData;
        try {
            userSecretData = unlock(secretData, secretToken, UserSecretData.newBuilder());
        } catch (IOException e) {
            log.debug("Error decoding secret data (likely because of wrong password)");
            return null;
        }

        // This stuff is deprecated because we sign the secret data
        if (userSecretData.hasDeprecatedVerifyPublicKey()) {
            // throw new IllegalStateException();

            log.warn("UserSecretData has deprecated verify public key");

            //
            // long hash =
            // Hashing.md5().hashBytes(user.getPublicKey().getEncoded().toByteArray()).asLong();
            // if (userSecretData.getDeprecatedVerifyPublicKey() != hash) {
            // // This is unexpected
            // log.warn("Verify public key did not match (but decode did not fail)");
            // return null;
            // }
        } else if (userSecretData.hasDeprecatedVerifyUserId()) {
            log.warn("UserSecretData has deprecated verify user id");

            // // This is deprecated because it makes creation painful. A
            // // pretty crappy reason, really.
            // log.warn("Falling back to deprecated user id validation for user: {}",
            // user.getName());
            //
            // if (userSecretData.getDeprecatedVerifyUserId() != user.getId()) {
            // // This is unexpected
            // log.warn("VerifyUserId did not match (but decode did not fail)");
            // return null;
            // }
        }

        return new UserWithSecret(user, userSecretData, secretToken);
    }

    public AuthenticatedProject authenticate(ProjectData project, AuthenticatedUser user) {
        ProjectRoles projectRoles = Users.findProjectRoles(user.getUserData(), project.getId());
        if (projectRoles == null) {
            // TODO: We probably need another path for domain admins
            return null;
        }

        if (!projectRoles.hasSecretData()) {
            throw new IllegalStateException("Project role has no secret data");
        }

        ProjectRolesSecretData projectRolesSecretData;
        try {
            projectRolesSecretData = unlock(projectRoles.getSecretData(), user, ProjectRolesSecretData.newBuilder());
        } catch (IOException e) {
            throw new IllegalStateException("Error unlocking project", e);
        }

        int version = 0;
        if (projectRolesSecretData.hasProjectKeyVersion()) {
            version = projectRolesSecretData.getProjectKeyVersion();
        }

        ByteString projectKeyBytes = projectRolesSecretData.getProjectKey();
        if (projectKeyBytes == null) {
            throw new IllegalStateException();
        }

        if (version == 1) {
            AesKey projectKey;
            try {
                projectKey = KeyczarUtils.unpack(projectKeyBytes.toByteArray());
            } catch (KeyczarException e) {
                throw new IllegalStateException("Error reading project key", e);
            }
            SecretToken token = new SecretToken(SecretTokenType.PROJECT_SECRET, projectKey, null);
            return new AuthenticatedProject(project, token);
        } else if (version == 0) {
            // We had a project key in version 0, but we didn't use it!
            return new AuthenticatedProject(project, null);
        } else {
            throw new IllegalStateException();
        }
    }

    public static SecretData buildProjectRolesSecret(UserData user, AuthenticatedProject authenticatedProject)
            throws KeyczarException {
        byte[] plaintext;
        {
            ProjectRolesSecretData.Builder b = ProjectRolesSecretData.newBuilder();
            byte[] serialized = KeyczarUtils.pack(authenticatedProject.getKeys().getSecretToken().cryptoKey);
            b.setProjectKey(ByteString.copyFrom(serialized));
            b.setProjectKeyVersion(1);

            plaintext = b.build().toByteArray();
        }

        Encrypter publicKeyEncrypter = getPublicKeyEncrypter(user.getPublicKey());
        byte[] ciphertext = publicKeyEncrypter.encrypt(plaintext);

        SecretData.Builder s = SecretData.newBuilder();
        s.setCiphertext(ByteString.copyFrom(ciphertext));
        s.setEncryptedWith(EncryptedWith.PUBLIC_KEY);
        s.setVersion(1);
        return s.build();
    }

    public static SecretData buildUserSecret(SecretToken userSecret, UserSecretData data) {
        byte[] plaintext = data.toByteArray();
        // CryptoKey userKey = userSecret.cryptoKey;

        byte[] ciphertext = userSecret.encrypt(plaintext);

        SecretData.Builder s = SecretData.newBuilder();
        s.setCiphertext(ByteString.copyFrom(ciphertext));
        s.setEncryptedWith(EncryptedWith.SECRET_KEY);
        s.setVersion(1);
        return s.build();
    }

    public static SecretData buildClientAppSecret(SecretToken secretToken, ClientAppSecretData data) {
        byte[] plaintext = data.toByteArray();

        byte[] ciphertext = secretToken.encrypt(plaintext);

        SecretData.Builder s = SecretData.newBuilder();
        s.setCiphertext(ByteString.copyFrom(ciphertext));
        s.setEncryptedWith(EncryptedWith.SECRET_KEY);
        s.setVersion(1);
        return s.build();
    }

    public static <T extends GeneratedMessage> T unlockWithSecretKey(ByteString secured, CryptoKey key,
            GeneratedMessage.Builder newBuilder) throws InvalidProtocolBufferException {
        byte[] plaintext = key.decrypt(secured.toByteArray());
        newBuilder.mergeFrom(plaintext);
        return (T) newBuilder.build();
    }

    // static <T extends GeneratedMessage> T unlockWithPrivateKey(ByteString
    // secured, PrivateKey key,
    // GeneratedMessage.Builder newBuilder) throws
    // InvalidProtocolBufferException {
    // byte[] plaintext = KeyPairs.decrypt(key, secured.toByteArray());
    // newBuilder.mergeFrom(plaintext);
    // return (T) newBuilder.build();
    // }

    static <T extends GeneratedMessage> T unlock(ByteString secured, Crypter crypter,
            GeneratedMessage.Builder newBuilder) throws IOException {
        byte[] plaintext;
        try {
            plaintext = crypter.decrypt(secured.toByteArray());
        } catch (KeyczarException e) {
            throw new IOException("Error decrypting data", e);
        }
        newBuilder.mergeFrom(plaintext);
        return (T) newBuilder.build();
    }

    public static <T extends GeneratedMessage> T unlock(ByteString secured, AesKey key,
            GeneratedMessage.Builder newBuilder) throws IOException {
        Crypter crypter = KeyczarUtils.buildCrypter(key);
        return unlock(secured, crypter, newBuilder);
    }

    static <T extends GeneratedMessage> T unlock(SecretData secured, AuthenticatedUser user,
            GeneratedMessage.Builder newBuilder) throws IOException {
        EncryptedWith encryptedWith = EncryptedWith.PUBLIC_KEY;

        if (secured.hasEncryptedWith()) {
            encryptedWith = secured.getEncryptedWith();
        }

        int version = 0;
        if (secured.hasVersion()) {
            version = secured.getVersion();
        }

        if (version == 0) {
            switch (encryptedWith) {
            case PUBLIC_KEY:
                // We could actually support this fairly easily...
                throw new IllegalStateException("Unwrapping with deprecated private key not supported");

                // log.warn("Using deprecated private key for user: {}", user);
                // return unlockWithPrivateKey(secured.getCiphertext(),
                // user.getKeys().getDeprecatedPrivateKey(),
                // newBuilder);

            case SECRET_KEY:
                return unlockWithSecretKey(secured.getCiphertext(), user.getKeys().getSecretToken().getDeprecatedKey(),
                        newBuilder);
            default:
                throw new IllegalArgumentException();
            }
        } else if (version == 1) {
            Crypter crypter;

            switch (encryptedWith) {
            case PUBLIC_KEY:
                crypter = user.getKeys().getAsymetricCrypter();
                break;
            case SECRET_KEY:
                crypter = user.getKeys().getSecretToken().getCrypter();
                break;
            default:
                throw new IllegalArgumentException();
            }

            return unlock(secured.getCiphertext(), crypter, newBuilder);

        } else {
            throw new IllegalArgumentException();
        }
    }

    public static <T extends GeneratedMessage> T unlock(SecretData secured, SecretToken secretToken,
            GeneratedMessage.Builder newBuilder) throws IOException {
        EncryptedWith encryptedWith = EncryptedWith.PUBLIC_KEY;

        if (secured.hasEncryptedWith()) {
            encryptedWith = secured.getEncryptedWith();
        }

        int version = 0;
        if (secured.hasVersion()) {
            version = secured.getVersion();
        }

        if (version == 0) {
            try {
                switch (encryptedWith) {
                case PUBLIC_KEY:
                    throw new IllegalArgumentException();
                case SECRET_KEY:
                    log.warn("Trying deprecated secret key");
                    return unlockWithSecretKey(secured.getCiphertext(), secretToken.getDeprecatedKey(), newBuilder);
                default:
                    throw new IllegalArgumentException();
                }
            } catch (Exception e) {
                log.warn("Error decrypting with v0, trying v1: {}", e.getMessage());
                version = 1;
            }
        }

        if (version == 1) {
            Crypter crypter;

            switch (encryptedWith) {
            case PUBLIC_KEY:
                throw new IllegalArgumentException();
            case SECRET_KEY:
                crypter = secretToken.getCrypter();
                break;
            default:
                throw new IllegalArgumentException();
            }

            return unlock(secured.getCiphertext(), crypter, newBuilder);
        } else {
            throw new IllegalArgumentException();
        }
    }

    public static Encrypter getPublicKeyEncrypter(KeyData keyData) throws KeyczarException {
        if (keyData.hasKeyczar()) {
            RsaPublicKey key;
            try {
                key = KeyczarUtils.readRsaPublicKey(keyData.getKeyczar());
            } catch (KeyczarException e) {
                throw new IllegalStateException("Error reading public key", e);
            }
            return new Encrypter(new KeyczarReaderWrapper(key));
        } else {
            throw new IllegalStateException("Only deprecated public key available");
        }
        // ByteString encoded = keyData.getEncoded();
        // if (encoded == null) {
        // throw new IllegalArgumentException();
        // }
        //
        // PublicKey publicKey =
        // KeyPairs.deserializePublicKey(encoded.toByteArray());
        //
        //
        // return publicKey;
    }

    public static KeyczarReaderWrapper getPrivateKey(KeyData keyData) {
        if (keyData.hasKeyczar()) {
            RsaPrivateKey key;
            try {
                key = KeyczarUtils.readRsaPrivateKey(keyData.getKeyczar());
            } catch (KeyczarException e) {
                throw new IllegalStateException("Error reading private key", e);
            }
            return new KeyczarReaderWrapper(key);
        } else {
            throw new IllegalStateException("Only deprecated private key available");
        }

        // if keyczar
        //
        //
        // ByteString encoded = keyData.getEncoded();
        // if (encoded == null) {
        // throw new IllegalArgumentException();
        // }
        //
        // PrivateKey privateKey =
        // KeyPairs.deserializePrivateKey(encoded.toByteArray());
        // return privateKey;
    }

    public void addPasswordAuth(UserData.Builder user, SecretToken userSecret, String password) {
        SecretStoreData.Builder secretStore = user.getSecretStoreBuilder();
        if (Strings.isNullOrEmpty(password)) {
            throw new IllegalArgumentException();
        }

        setPassword(secretStore, password, userSecret);
    }

    public void addPublicKeyAuth(UserData.Builder user, String credentialKey, ByteString challenge) {
        if (Strings.isNullOrEmpty(credentialKey)) {
            throw new IllegalArgumentException();
        }

        if (challenge == null) {
            throw new IllegalArgumentException();
        }

        SecretStoreData.Builder secretStore = user.getSecretStoreBuilder();
        SecretKeyData.Builder b = getSecretKeyData(secretStore, SecretKeyType.ENCRYPTED_WITH_CREDENTIAL);

        b.setCredentialKey(credentialKey);

        b.setCiphertext(challenge);
    }

    public void addTokenRecovery(UserData.Builder user, SecretToken secret) {
        SecretStoreData.Builder secretStore = user.getSecretStoreBuilder();

        SecretKeyData.Builder b = getSecretKeyData(secretStore, SecretKeyType.ENCRYPTED_WITH_FORGOTPASSWORD_PUBKEY);

        Encrypter encrypter = keystore.buildEncrypter(KEY_FORGOT_PASSWORD_PUBLIC);

        b.setVersion(1);

        byte[] serialized = KeyczarUtils.pack(secret.cryptoKey);
        byte[] ciphertext;
        try {
            ciphertext = encrypter.encrypt(serialized);
        } catch (KeyczarException e) {
            throw new IllegalStateException("Error encrypting key", e);
        }

        b.setCiphertext(ByteString.copyFrom(ciphertext));
    }

    public static void storeLockedByProject(SecretStoreData.Builder data, AuthenticatedProject project,
            SecretToken secret) {
        SecretKeyData.Builder b = getSecretKeyData(data, SecretKeyType.ENCRYPTED_WITH_PROJECT_KEY);

        SecretToken secretToken = project.getKeys().getSecretToken();

        if (secretToken.type != SecretTokenType.PROJECT_SECRET) {
            throw new IllegalArgumentException();
        }

        Encrypter encrypter = secretToken.getCrypter();

        b.setVersion(1);

        byte[] serialized = KeyczarUtils.pack(secret.cryptoKey);
        byte[] ciphertext;
        try {
            ciphertext = encrypter.encrypt(serialized);
        } catch (KeyczarException e) {
            throw new IllegalStateException("Error encrypting key", e);
        }

        b.setCiphertext(ByteString.copyFrom(ciphertext));
    }

    private static SecretKeyData.Builder getSecretKeyData(SecretStoreData.Builder data, SecretKeyType secretKeyType) {
        for (SecretKeyData.Builder entry : data.getSecretKeyBuilderList()) {
            if (entry.getType() != secretKeyType) {
                continue;
            }

            return entry;
        }

        SecretKeyData.Builder b = data.addSecretKeyBuilder();
        b.setType(secretKeyType);
        return b;
    }

    public static void setPassword(SecretStoreData.Builder data, String password, AuthenticatedUserKeys secret) {
        setPassword(data, password, secret.getSecretToken());
    }

    public static void setPassword(SecretStoreData.Builder data, String password, SecretToken secret) {
        SecretKeyData.Builder b = getSecretKeyData(data, SecretKeyType.ENCRYPTED_WITH_USER_PASSWORD);

        // b.setUserId(value);
        b.setIterations(2000);

        {
            byte[] seed = KeyczarUtils.generateSecureRandom(16);
            b.setSeed(ByteString.copyFrom(seed));
        }

        AesKey passwordKey = KeyczarUtils.deriveKey(b.getIterations(), b.getSeed().toByteArray(), password);

        b.setVersion(1);
        byte[] ciphertext = encryptSymetricKey(passwordKey, secret.cryptoKey);
        b.setCiphertext(ByteString.copyFrom(ciphertext));
    }

    public static SecretToken getSecretFromPassword(SecretStoreData data, final String password)
            throws KeyczarException {
        for (SecretKeyData entry : data.getSecretKeyList()) {
            if (entry.getType() != SecretKeyType.ENCRYPTED_WITH_USER_PASSWORD) {
                continue;
            }

            // if (entry.getUserId() != userId) {
            // continue;
            // }

            int version = 0;
            if (entry.hasVersion()) {
                version = entry.getVersion();
            }

            CryptoKey v0;
            AesKey v1;

            if (version == 0) {
                AesCbcCryptoKey passwordKey = AesCbcCryptoKey.deriveKey(entry.getIterations(), entry.getSeed()
                        .toByteArray(), password);
                byte[] plaintext = FathomdbCrypto.decrypt(passwordKey, entry.getCiphertext().toByteArray());
                v0 = FathomdbCrypto.deserializeKey(plaintext);

                HmacKey hmacKey = KeyczarUtils.deriveHmac(plaintext, entry.getSeed().toByteArray(), password);
                v1 = new AesKey(((AesCbcCryptoKey) v0).getJce().getEncoded(), hmacKey);
            } else if (version == 1) {
                AesKey passwordKey = KeyczarUtils.deriveKey(entry.getIterations(), entry.getSeed().toByteArray(),
                        password);

                byte[] plaintext = KeyczarUtils.decrypt(passwordKey, entry.getCiphertext().toByteArray());

                v1 = KeyczarUtils.unpack(plaintext);
                v0 = null;
            } else {
                throw new IllegalStateException();
            }

            return new SecretToken(SecretTokenType.USER_SECRET, v1, v0);
        }

        return null;
    }

    // private static CryptoKey decryptSymetricKey(CryptoKey key, ByteString
    // ciphertext) {
    // return toSecretKey(FathomdbCrypto.decrypt(key,
    // ciphertext.toByteArray()));
    // }

    //
    // private static CryptoKey toSecretKey(byte[] keyData) {
    // return FathomdbCrypto.deserializeKey(keyData);
    // }

    private static byte[] encryptSymetricKey(AesKey passwordKey, AesKey secret) {
        byte[] serialized = KeyczarUtils.pack(secret);

        return KeyczarUtils.encrypt(passwordKey, serialized);
    }

    public UserWithSecret checkPublicKey(UserData user, CredentialData credential, ClientCertificate clientCertificate,
            ByteString challenge, ByteString responseData) {
        AesKey secretKey;

        if (!ChallengeResponses.hasPrefix(responseData.toByteArray())) {
            log.warn("Challenge response was not valid");
            return null;
        }

        byte[] payload = ChallengeResponses.getPayload(responseData.toByteArray());
        try {
            secretKey = KeyczarUtils.unpack(payload);
        } catch (KeyczarException e) {
            log.warn("Error unpacking key", e);
            return null;
        }

        SecretToken secretToken = new SecretToken(SecretTokenType.USER_SECRET, secretKey, null);
        // if (secretToken == null) {
        // return null;
        // }

        UserWithSecret ret = checkSecret(user, secretToken);
        return ret;
    }

    public ByteString buildAuthChallenge(UserData user, CredentialData credential, ClientCertificate clientCertificate) {
        for (SecretKeyData secretKey : user.getSecretStore().getSecretKeyList()) {
            SecretKeyType type = secretKey.getType();
            if (type != SecretKeyType.ENCRYPTED_WITH_CREDENTIAL) {
                continue;
            }

            if (!Objects.equal(secretKey.getCredentialKey(), credential.getKey())) {
                continue;
            }

            ByteString ciphertext = secretKey.getCiphertext();

            // TODO: Encrypt?? I think this would need a commutative encryption
            // algorithm.
            // We could also return a pair of challenges, one repeated, one not.

            return ByteString.copyFrom(ChallengeResponses.addHeader(ciphertext.toByteArray()));
        }

        log.warn("Unable to build auth challenge for credential: {}", credential);

        return null;
    }

}
