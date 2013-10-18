package io.fathom.auto.config;

import java.io.IOException;
import java.util.concurrent.locks.Lock;

public abstract class ConfigPath {

    public abstract ConfigPath child(String name);

    public abstract Iterable<ConfigEntry> listChildren() throws IOException;

    public final String readChild(String name) throws IOException {
        return child(name).read();
    }

    public abstract String read() throws IOException;

    public abstract void write(String contents) throws IOException;

    public abstract void delete() throws IOException;

    public abstract Lock buildLock();
}
