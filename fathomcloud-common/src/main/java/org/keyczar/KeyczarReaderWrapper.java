package org.keyczar;

import java.util.ArrayList;
import java.util.List;

import org.keyczar.enums.KeyPurpose;
import org.keyczar.enums.KeyStatus;
import org.keyczar.exceptions.KeyczarException;
import org.keyczar.interfaces.KeyczarReader;

public class KeyczarReaderWrapper implements KeyczarReader {
    private final KeyMetadata metadata;
    private final List<KeyczarKey> keys;

    public KeyczarReaderWrapper(RsaPublicKey key) {
        this.metadata = new KeyMetadata("Imported RSA", KeyPurpose.ENCRYPT, DefaultKeyType.RSA_PUB);
        KeyVersion version = new KeyVersion(0, KeyStatus.PRIMARY, false);
        this.metadata.addVersion(version);
        this.keys = new ArrayList<KeyczarKey>();
        this.keys.add(key);
    }

    public KeyczarReaderWrapper(RsaPrivateKey key) {
        this.metadata = new KeyMetadata("Imported RSA", KeyPurpose.DECRYPT_AND_ENCRYPT, DefaultKeyType.RSA_PRIV);
        KeyVersion version = new KeyVersion(0, KeyStatus.PRIMARY, false);
        this.metadata.addVersion(version);
        this.keys = new ArrayList<KeyczarKey>();
        this.keys.add(key);
    }

    @Override
    public String getKey() throws KeyczarException {
        KeyMetadata metadata = KeyMetadata.read(getMetadata());

        return getKey(metadata.getPrimaryVersion().getVersionNumber());
    }

    @Override
    public String getKey(int version) {
        return keys.get(version).toString();
    }

    @Override
    public String getMetadata() {
        String s = metadata.toString();
        return s;
    }

}
