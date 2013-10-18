package io.fathom.auto.locks;

import io.fathom.auto.TimeSpan;
import io.fathom.auto.config.ConfigEntry;
import io.fathom.auto.config.ConfigPath;

import java.io.IOException;
import java.util.List;

import com.google.common.collect.Lists;

public class OpenstackPseudoLock extends PseudoLockBase {
    private final ConfigPath path;

    public OpenstackPseudoLock(ConfigPath path, TimeSpan timeoutMs, TimeSpan pollingMs) {
        super(timeoutMs, pollingMs);
        this.path = path;
    }

    public OpenstackPseudoLock(ConfigPath path, TimeSpan timeoutMs, TimeSpan pollingMs, TimeSpan settlingMs) {
        super(timeoutMs, pollingMs, settlingMs);
        this.path = path;
    }

    @Override
    protected void createFile(String key, String contents) throws IOException {
        ConfigPath childPath = path.child(key);
        try {
            childPath.write(contents);
        } catch (IOException e) {
            throw new IOException("Error writing cloud file", e);
        }
    }

    @Override
    protected void deleteFile(String key) throws IOException {
        ConfigPath childPath = path.child(key);
        try {
            childPath.delete();
        } catch (IOException e) {
            throw new IOException("Error deleting cloud file", e);
        }
    }

    @Override
    protected List<String> getFileNames() throws IOException {
        List<String> ret = Lists.newArrayList();

        try {
            Iterable<ConfigEntry> children = path.listChildren();
            if (children != null) {
                for (ConfigEntry o : children) {
                    String key = o.getName();
                    ret.add(key);
                }
            }
        } catch (IOException e) {
            throw new IOException("Error listing cloud files", e);
        }

        return ret;
    }

    @Override
    public String toString() {
        return "OpenstackPseudoLock [path=" + path + "]";
    }

}
