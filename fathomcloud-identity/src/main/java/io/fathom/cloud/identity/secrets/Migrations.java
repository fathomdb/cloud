package io.fathom.cloud.identity.secrets;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.PasswordHasher;
import io.fathom.cloud.identity.secrets.SecretToken.SecretTokenType;
import io.fathom.cloud.identity.state.AuthRepository;
import io.fathom.cloud.protobuf.IdentityModel.CredentialData;
import io.fathom.cloud.protobuf.IdentityModel.SecretKeyData;
import io.fathom.cloud.protobuf.IdentityModel.SecretKeyType;
import io.fathom.cloud.protobuf.IdentityModel.SecretStoreData;
import io.fathom.cloud.protobuf.IdentityModel.UserData;
import io.fathom.cloud.protobuf.IdentityModel.UserSecretData;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.keyczar.DefaultKeyType;
import org.keyczar.KeyMetadata;
import org.keyczar.KeyczarKey;
import org.keyczar.KeyczarUtils;
import org.keyczar.enums.KeyPurpose;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.inject.persist.Transactional;

@Singleton
@Transactional
public class Migrations {

    private static final Logger log = LoggerFactory.getLogger(Migrations.class);

    public static void report(Object o) {
        log.warn("Temporary migration for: {}", o);
    }

    @Inject
    AuthRepository authRepository;

    @Inject
    @Deprecated
    PasswordHasher hasher;

    @Inject
    Secrets secretService;

    public UserWithSecret migrateUserAddPasswordRecovery(UserWithSecret user, SecretToken secretToken)
            throws CloudException {
        UserSecretData userSecretData = user.userSecretData;
        UserData userData = user.getUserData();

        for (SecretKeyData entry : userData.getSecretStore().getSecretKeyList()) {
            if (entry.getType() == SecretKeyType.ENCRYPTED_WITH_FORGOTPASSWORD_PUBKEY) {
                return user;
            }
        }

        log.info("Migrating user - adding password recovery: {}", user.getUserData());
        report(userData);

        UserData.Builder userDataBuilder = UserData.newBuilder(userData);
        UserSecretData.Builder userSecretDataBuilder = UserSecretData.newBuilder(userSecretData);

        secretService.addTokenRecovery(userDataBuilder, secretToken);

        userData = authRepository.getUsers().update(userDataBuilder);
        return new UserWithSecret(userData, userSecretData, secretToken);
    }

    public UserData migrateUser(CredentialData credential, UserData user, String password) throws CloudException {
        if (!user.hasSecretStore()) {
            if (hasher.isValid(credential.getDeprecatedPasswordHash(), password)) {
                log.warn("Migrating user password to new secure store");
                SecretToken userSecret = SecretToken.create(SecretTokenType.USER_SECRET);

                UserData.Builder b = UserData.newBuilder(user);

                SecretStoreData.Builder secretStore = b.getSecretStoreBuilder();
                if (!Strings.isNullOrEmpty(password)) {
                    Secrets.setPassword(secretStore, password, userSecret);
                } else {
                    // Without a password, there's going to be no way to get the
                    // key
                    throw new UnsupportedOperationException();
                }

                // KeyPair keypair = KeyPairs.generateKeyPair();
                // byte[] publicKey = keypair.getPublic().getEncoded();
                // byte[] privateKey = keypair.getPrivate().getEncoded();
                //
                // b.getPublicKeyBuilder().setEncoded(ByteString.copyFrom(publicKey));

                if (b.hasSecretData()) {
                    throw new IllegalStateException();
                }

                UserSecretData.Builder s = UserSecretData.newBuilder();
                // // s.setVerifyUserId(b.getId());
                // s.getPrivateKeyBuilder().setEncoded(ByteString.copyFrom(privateKey));
                //
                b.setSecretData(Secrets.buildUserSecret(userSecret, s.build()));

                user = authRepository.getUsers().update(b);
            }
        }
        return user;
    }

    public UserWithSecret migrateUser(UserWithSecret user, String password, SecretToken secretToken)
            throws CloudException {
        UserSecretData userSecretData = user.userSecretData;
        UserData userData = user.getUserData();

        if (!userData.getPublicKey().hasKeyczar()) {
            log.info("Migrating user to keyczar: {}", user.getUserData());
            Migrations.report(userData);

            UserData.Builder userDataBuilder = UserData.newBuilder(userData);
            UserSecretData.Builder userSecretDataBuilder = UserSecretData.newBuilder(userSecretData);

            KeyczarKey keypair = KeyczarUtils.createKey(new KeyMetadata("RSA Key", KeyPurpose.DECRYPT_AND_ENCRYPT,
                    DefaultKeyType.RSA_PRIV));

            if (userSecretDataBuilder.getPrivateKeyBuilder().hasKeyczar()) {
                throw new IllegalStateException();
            }

            userSecretDataBuilder.getPrivateKeyBuilder().setKeyczar(keypair.toString());

            if (userDataBuilder.getPublicKeyBuilder().hasKeyczar()) {
                throw new IllegalStateException();
            }

            userDataBuilder.getPublicKeyBuilder().setKeyczar(KeyczarUtils.getPublicKey(keypair).toString());

            SecretStoreData.Builder secretStore = userDataBuilder.getSecretStoreBuilder();
            if (!Strings.isNullOrEmpty(password)) {
                Secrets.setPassword(secretStore, password, secretToken);
            } else {
                // Without a password, there's going to be no way to get the key
                throw new UnsupportedOperationException();
            }

            userSecretData = userSecretDataBuilder.build();
            userDataBuilder.setSecretData(Secrets.buildUserSecret(secretToken, userSecretData));

            userData = authRepository.getUsers().update(userDataBuilder);
            return new UserWithSecret(userData, userSecretData, secretToken);
        }

        return user;
    }

}
