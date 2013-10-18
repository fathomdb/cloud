package io.fathom.auto.zookeeper;

import io.fathom.auto.zookeeper.model.ClusterState;
import io.fathom.auto.zookeeper.model.ZookeeperClusterRegistration;

import java.util.Map;

public class ClusterSnapshot {
    final ClusterState state;
    final Map<Integer, ZookeeperClusterRegistration> servers;

    public ClusterSnapshot(ClusterState state, Map<Integer, ZookeeperClusterRegistration> servers) {
        this.state = state;
        this.servers = servers;
    }

    public ClusterState getClusterState() {
        return state;
    }

}
