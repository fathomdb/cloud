package io.fathom.auto.config;

public class ConfigEntry {

    final String name;
    final long version;

    public ConfigEntry(String name, long version) {
        this.name = name;
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public long getVersion() {
        return version;
    }

}
