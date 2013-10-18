package io.fathom.auto.zookeeper.model;

import io.fathom.auto.JsonCodec;

public class ZookeeperClusterRegistration {
    public static final String PARTICIPANT = "participant";
    public static final String OBSERVER = "observer";

    public int serverId;
    public String ip;
    public String type;
    public String signature;

    public boolean isObserver() {
        return OBSERVER.equalsIgnoreCase(type);
    }

    @Override
    public String toString() {
        return JsonCodec.formatJson(this);
    }

}
