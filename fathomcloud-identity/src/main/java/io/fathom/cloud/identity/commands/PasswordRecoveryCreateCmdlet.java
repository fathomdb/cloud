package io.fathom.cloud.identity.commands;

import io.fathom.cloud.commands.Cmdlet;
import io.fathom.cloud.identity.LoginService;
import io.fathom.cloud.identity.secrets.Secrets;
import io.fathom.cloud.identity.services.IdentityService;
import io.fathom.cloud.keyczar.KeyczarFactory;

import java.io.File;

import javax.inject.Inject;

import org.keyczar.Crypter;
import org.keyczar.DefaultKeyType;
import org.keyczar.GenericKeyczar;
import org.keyczar.KeyMetadata;
import org.keyczar.KeyVersion;
import org.keyczar.KeyczarFileReader;
import org.keyczar.enums.KeyPurpose;
import org.keyczar.enums.KeyStatus;
import org.keyczar.enums.RsaPadding;
import org.keyczar.exceptions.KeyczarException;
import org.keyczar.keyparams.KeyParameters;
import org.keyczar.keyparams.RsaKeyParameters;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PasswordRecoveryCreateCmdlet extends Cmdlet {
    public PasswordRecoveryCreateCmdlet() {
        super("id-passwordrecovery-create");
    }

    private static final Logger log = LoggerFactory.getLogger(PasswordRecoveryCreateCmdlet.class);

    @Inject
    IdentityService identityService;

    @Inject
    LoginService loginService;

    @Inject
    KeyczarFactory keyczarFactory;

    @Option(name = "-o", usage = "path for private key store", required = true)
    public File path;

    @Override
    public void run() throws Exception {
        log.info("Checking for existing password-recovery key");
        {
            Crypter crypter = null;
            GenericKeyczar store = keyczarFactory.find(Secrets.KEY_FORGOT_PASSWORD_PUBLIC, crypter);
            if (store != null) {
                // TODO: Should we allow key rotation? Replacement?
                log.info("Password-recovery key already exists");
                return;
            }
        }

        log.info("Creating password recovery key");

        String nameFlag = "Password recovery keystore";

        String metadata = null;

        {
            path.mkdirs();

            KeyczarFileReader store = new KeyczarFileReader(path.getAbsolutePath());

            try {
                metadata = store.getMetadata();
            } catch (Exception e) {
                log.info("Metadata not found");
            }

            if (metadata == null) {
                KeyMetadata kmd = new KeyMetadata(nameFlag, KeyPurpose.DECRYPT_AND_ENCRYPT, DefaultKeyType.RSA_PRIV);
                GenericKeyczar.create(store, kmd);
            }
        }

        {
            KeyczarFileReader store = new KeyczarFileReader(path.getAbsolutePath());

            GenericKeyczar keyczar = new GenericKeyczar(store);

            for (KeyVersion version : keyczar.getVersions()) {
                log.info("Local password recovery key already exists; exiting for safety");
                return;
            }

            KeyParameters keyParameters = DefaultKeyType.RSA_PRIV.applyDefaultParameters(new RsaKeyParameters() {
                @Override
                public int getKeySize() throws KeyczarException {
                    return 4096;
                }

                @Override
                public RsaPadding getRsaPadding() throws KeyczarException {
                    // Use default
                    return null;
                }
            });
            keyczar.addVersion(KeyStatus.PRIMARY, keyParameters);
            keyczar.write();

            log.info("Storing public key in zookeeper");

            keyczarFactory.publicKeyExport(Secrets.KEY_FORGOT_PASSWORD_PUBLIC, keyczar);

            log.info("Saved public key in zookeeper");
            log.info("Store the private key in a _very_ safe place");
            log.info("Backup the entire directory: {}", path);
        }
    }
}
