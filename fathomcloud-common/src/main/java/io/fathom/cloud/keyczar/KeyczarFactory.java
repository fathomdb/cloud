package io.fathom.cloud.keyczar;

import org.keyczar.Crypter;
import org.keyczar.GenericKeyczar;
import org.keyczar.KeyMetadata;
import org.keyczar.exceptions.KeyczarException;
import org.keyczar.interfaces.KeyczarReader;

public interface KeyczarFactory {
    KeyczarReader getReader(String key) throws KeyczarException;

    KeyczarReader getReader(String key, Crypter crypter) throws KeyczarException;

    GenericKeyczar find(String keystoreId, Crypter crypter) throws KeyczarException;

    GenericKeyczar create(String keystoreId, KeyMetadata kmd, Crypter crypter) throws KeyczarException;

    /**
     * Makes sure that there is an active key.
     * 
     * @return true if something changed (i.e. key created)
     */
    boolean ensureKeyCreated(GenericKeyczar store) throws KeyczarException;

    void publicKeyExport(String dest, GenericKeyczar serviceSecret) throws KeyczarException;
}
