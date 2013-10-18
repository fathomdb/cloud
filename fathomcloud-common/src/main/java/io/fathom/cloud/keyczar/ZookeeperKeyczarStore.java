package io.fathom.cloud.keyczar;

import io.fathom.cloud.zookeeper.ZookeeperClient;

import java.io.IOException;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;
import org.keyczar.KeyczarStore;
import org.keyczar.exceptions.KeyczarException;
import org.keyczar.i18n.Messages;

import com.google.common.base.Charsets;

public class ZookeeperKeyczarStore extends KeyczarStore {
    static final String META_FILE = "meta";

    final ZookeeperClient zk;
    final String location;

    public ZookeeperKeyczarStore(ZookeeperClient zk, String location) {
        this.zk = zk;
        if (!location.endsWith("/")) {
            location += "/";
        }

        this.location = location;
    }

    @Override
    public String getMetadata() throws KeyczarException {
        return readFile(location + META_FILE);
    }

    @Override
    public String getKey(int version) throws KeyczarException {
        return readFile(location + version);
    }

    private String readFile(String path) throws KeyczarException {
        try {
            Stat stat = new Stat();
            byte[] data = zk.getData(path, null, stat);
            return new String(data, Charsets.UTF_8);
        } catch (IOException e) {
            throw new KeyczarException("Error reading key", e);
        } catch (KeeperException e) {
            throw new KeyczarException("Error reading key", e);
        }
    }

    private boolean exists(String path) throws KeyczarException {
        try {
            Stat stat = zk.exists(path, false);
            return stat != null;
        } catch (IOException e) {
            throw new KeyczarException("Error reading key", e);
        } catch (KeeperException e) {
            throw new KeyczarException("Error reading key", e);
        }
    }

    void writeFile(String path, byte[] data) throws KeyczarException {
        try {
            try {
                zk.createOrUpdate(path, data, true);
            } catch (KeeperException e) {
                throw new IOException("Error writing key", e);
            }
        } catch (IOException e) {
            throw new KeyczarException(Messages.getString("KeyczarTool.UnableToWrite", location), e);
        }
    }

    public boolean hasMetadata() throws KeyczarException {
        return exists(location + META_FILE);
    }

    @Override
    public void setMetadata(String metadata) throws KeyczarException {
        writeFile(location + META_FILE, metadata.getBytes(Charsets.UTF_8));
    }

    @Override
    public void setKey(int versionNumber, String key) throws KeyczarException {
        writeFile(location + Integer.toString(versionNumber), key.getBytes(Charsets.UTF_8));
    }
}
