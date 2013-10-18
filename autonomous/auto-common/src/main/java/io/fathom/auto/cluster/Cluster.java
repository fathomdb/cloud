package io.fathom.auto.cluster;

import io.fathom.auto.JsonCodec;
import io.fathom.auto.TimeSpan;
import io.fathom.auto.config.ConfigEntry;
import io.fathom.auto.config.ConfigPath;
import io.fathom.auto.config.MachineInfo;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public abstract class Cluster<T> {
    private static final Logger log = LoggerFactory.getLogger(Cluster.class);

    protected final ConfigPath base;

    private final Lock lock;

    public Cluster(ConfigPath base, Lock lock) {
        this.base = base;
        this.lock = lock;
    }

    public void writeRegistration(int serverId, T registration) throws IOException {
        ConfigPath node = getServerPath(serverId);

        String json = JsonCodec.gson.toJson(registration);
        node.write(json);
    }

    public List<Integer> getServerIds() throws IOException {
        ConfigPath servers = base.child("servers");
        List<Integer> serverIds = Lists.newArrayList();

        Iterable<ConfigEntry> children = servers.listChildren();
        if (children == null) {
            // Not found
            return null;
        }

        for (ConfigEntry o : children) {
            String key = o.getName();
            serverIds.add(Integer.valueOf(key));
        }

        return serverIds;
    }

    private ConfigPath getServerPath(int serverId) {
        ConfigPath servers = base.child("servers");
        ConfigPath me = servers.child("" + serverId);
        return me;
    }

    private T readServerRegistration(int serverId) throws IOException {
        ConfigPath node = getServerPath(serverId);

        String json = node.read();
        if (json == null) {
            return null;
        }

        T registration = deserialize(json);
        return registration;
    }

    protected abstract T deserialize(String json);

    public Map<Integer, T> getServers() throws IOException {
        Map<Integer, T> servers = Maps.newHashMap();
        List<Integer> serverIds = getServerIds();
        if (serverIds != null) {
            for (int serverId : serverIds) {
                T serverRegistration = readServerRegistration(serverId);
                servers.put(serverId, serverRegistration);
            }
        }

        return servers;
    }

    T me;

    protected T findMe(String signature) throws IOException {
        if (me == null) {
            Map<Integer, T> servers = getServers();
            me = findMe(signature, servers);
        }

        return me;
    }

    private T findMe(String signature, Map<Integer, T> servers) {
        for (T server : servers.values()) {
            if (signature.equals(getSignature(server))) {
                return server;
            }
        }
        return null;
    }

    String signature;

    protected String getSignature() {
        if (signature == null) {
            MachineInfo machineInfo = MachineInfo.INSTANCE;
            signature = machineInfo.getMachineKey();
        }
        return signature;
    }

    public T register() throws IOException {
        while (true) {
            log.info("Determining server id");

            T me = findMe(getSignature());
            if (me != null) {
                log.info("Found server: {}", me);
                return me;
            }

            // TODO: Strictly we don't need to take the global lock here...
            Lock pseudoLock = getLock();

            try {
                if (pseudoLock.tryLock(1, TimeUnit.MINUTES)) {
                    // We've got the lock...
                    Map<Integer, T> servers = getServers();

                    T found = findMe(signature, servers);
                    if (found == null) {
                        int serverId = 1;
                        while (servers.containsKey(serverId)) {
                            serverId++;
                        }

                        T registration = buildRegistration(serverId);
                        writeRegistration(serverId, registration);
                        me = registration;
                        log.info("Registered server: {}", me);
                    } else {
                        me = found;
                    }
                    return me;
                } else {
                    log.info("Unable to obtain lock; retrying");
                }
            } catch (Exception e) {
                log.warn("Error getting server id", e);
                pseudoLock.unlock();
                TimeSpan.seconds(10).sleep();
                continue;
            } finally {
                pseudoLock.unlock();
            }
        }

    }

    protected abstract T buildRegistration(int serverId);

    protected abstract String getSignature(T server);

    public Lock getLock() {
        return lock;
    }

}
