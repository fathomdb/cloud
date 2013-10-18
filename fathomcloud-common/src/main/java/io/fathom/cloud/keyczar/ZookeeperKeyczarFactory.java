package io.fathom.cloud.keyczar;

import io.fathom.cloud.zookeeper.ZookeeperClient;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.keyczar.Crypter;
import org.keyczar.GenericKeyczar;
import org.keyczar.KeyMetadata;
import org.keyczar.KeyVersion;
import org.keyczar.KeyczarEncryptedWriter;
import org.keyczar.enums.KeyStatus;
import org.keyczar.exceptions.KeyczarException;
import org.keyczar.interfaces.KeyczarReader;
import org.keyczar.interfaces.KeyczarWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathomdb.Configuration;

@Singleton
public class ZookeeperKeyczarFactory implements KeyczarFactory {
    private static final Logger log = LoggerFactory.getLogger(ZookeeperKeyczarFactory.class);

    final ZookeeperClient zk;
    final String base;

    public ZookeeperKeyczarFactory(ZookeeperClient zk, String base) {
        this.zk = zk;

        if (!base.endsWith("/")) {
            base += "/";
        }
        this.base = base;
    }

    @Inject
    public ZookeeperKeyczarFactory(ZookeeperClient zk, Configuration config) {
        this(zk, config.lookup("keystore.zookeeper.path", "/keystore"));
    }

    public GenericKeyczar find(String key) throws KeyczarException {
        return find(key, null);
    }

    @Override
    public GenericKeyczar find(String key, Crypter crypter) throws KeyczarException {
        KeyczarWriter reader = getReader(key, crypter);
        if (!hasMetadata(reader)) {
            return null;
        }
        return new GenericKeyczar(reader);
    }

    @Override
    public KeyczarWriter getReader(String key, Crypter crypter) throws KeyczarException {
        String location = base + key;

        KeyczarWriter reader = buildStore(zk, location, crypter);
        return reader;
    }

    @Override
    public GenericKeyczar create(String key, KeyMetadata kmd, Crypter crypter) throws KeyczarException {
        String location = base + key;

        KeyczarWriter reader = buildStore(zk, location, crypter);
        if (hasMetadata(reader)) {
            throw new IllegalStateException();
        }

        GenericKeyczar keyczar = GenericKeyczar.create(reader, kmd);
        return keyczar;

    }

    private boolean hasMetadata(KeyczarWriter reader) {
        try {
            reader.getMetadata();
            return true;
        } catch (KeyczarException e) {
            return false;
        }
    }

    private KeyczarWriter buildStore(ZookeeperClient zk, String location, Crypter crypter) {
        ZookeeperKeyczarStore store = new ZookeeperKeyczarStore(zk, location);
        if (crypter == null) {
            return store;
        } else {
            return new KeyczarEncryptedWriter(store, crypter, crypter);
        }
    }

    @Override
    public boolean ensureKeyCreated(GenericKeyczar store) throws KeyczarException {
        KeyVersion primaryVersion = store.getMetadata().findPrimaryVersion();
        if (primaryVersion != null) {
            return false;
        }

        log.info("Creating new KeyCzar key");
        store.addVersion(KeyStatus.PRIMARY);

        store.write();

        return true;
    }

    @Override
    public void publicKeyExport(String dest, GenericKeyczar src) throws KeyczarException {
        String destLocation = base + dest;
        src.publicKeyExport(new ZookeeperKeyczarStore(zk, destLocation));
    }

    @Override
    public KeyczarReader getReader(String key) throws KeyczarException {
        return getReader(key, null);
    }

}
