package io.fathom.auto.zookeeper;

import io.fathom.auto.JsonCodec;
import io.fathom.auto.cluster.Cluster;
import io.fathom.auto.config.ConfigPath;
import io.fathom.auto.config.MachineInfo;
import io.fathom.auto.zookeeper.model.ClusterState;
import io.fathom.auto.zookeeper.model.ZookeeperClusterRegistration;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import com.google.common.net.InetAddresses;

public class ZookeeperCluster extends Cluster<ZookeeperClusterRegistration> {

    public ZookeeperCluster(ConfigPath base, Lock lock) {
        super(base, lock);
    }

    public void writeClusterState(ClusterState state) throws IOException {
        ConfigPath node = getStatePath();

        String json = JsonCodec.gson.toJson(state);
        node.write(json);
    }

    public ClusterState readClusterState() throws IOException {
        String json = getStatePath().read();
        if (json == null) {
            return null;
        }
        return JsonCodec.gson.fromJson(json, ClusterState.class);
    }

    public ClusterSnapshot getSnapshot() throws IOException {
        ClusterState state = readClusterState();

        Map<Integer, ZookeeperClusterRegistration> servers = getServers();

        return new ClusterSnapshot(state, servers);
    }

    private ConfigPath getStatePath() {
        return base.child("state");
    }

    @Override
    protected ZookeeperClusterRegistration deserialize(String json) {
        return JsonCodec.gson.fromJson(json, ZookeeperClusterRegistration.class);
    }

    @Override
    protected String getSignature(ZookeeperClusterRegistration server) {
        return server.signature;
    }

    @Override
    protected ZookeeperClusterRegistration buildRegistration(int myid) {
        InetAddress ip = MachineInfo.INSTANCE.getIp();

        ZookeeperClusterRegistration registration = new ZookeeperClusterRegistration();
        registration.ip = InetAddresses.toAddrString(ip);
        registration.serverId = myid;

        // We start up as an observer, and then we try to upgrade to participant
        registration.type = ZookeeperClusterRegistration.OBSERVER;
        registration.signature = getSignature();

        return registration;
    }

}
