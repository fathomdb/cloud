package io.fathom.cloud.state.zookeeper;

import io.fathom.cloud.keyczar.KeyczarFactory;
import io.fathom.cloud.server.auth.SharedKeystore;

import javax.inject.Inject;

import org.keyczar.Crypter;
import org.keyczar.Encrypter;
import org.keyczar.GenericKeyczar;
import org.keyczar.KeyMetadata;
import org.keyczar.Signer;
import org.keyczar.exceptions.KeyczarException;
import org.keyczar.interfaces.KeyczarReader;

public class KeyczarSharedKeystore implements SharedKeystore {

    @Inject
    KeyczarFactory keyczarFactory;

    @Override
    public Signer buildSigner(String key) {
        try {
            KeyczarReader child = keyczarFactory.getReader(key);
            return new Signer(child);
        } catch (KeyczarException e) {
            throw new IllegalStateException("Error building signer", e);
        }
    }

    @Override
    public Crypter buildCrypter(String key) {
        try {
            KeyczarReader child = keyczarFactory.getReader(key);
            return new Crypter(child);
        } catch (KeyczarException e) {
            throw new IllegalStateException("Error building crypter", e);
        }
    }

    @Override
    public Encrypter buildEncrypter(String key) {
        try {
            KeyczarReader child = keyczarFactory.getReader(key);
            return new Encrypter(child);
        } catch (KeyczarException e) {
            throw new IllegalStateException("Error building encrypter", e);
        }
    }

    @Override
    public void ensureCreated(String key, KeyMetadata keyMetadata) {
        try {
            Crypter crypter = null;
            GenericKeyczar store = keyczarFactory.find(key, crypter);
            if (store == null) {
                store = keyczarFactory.create(key, keyMetadata, crypter);
            }
            keyczarFactory.ensureKeyCreated(store);
        } catch (KeyczarException e) {
            throw new IllegalStateException("Error creating keystore", e);
        }
    }
}
