package io.fathom.cloud.blobs.replicated;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;

public class StorageCluster {
    final ConsistentHash<StorageNode> ring;
    final String etag;
    final int dataReplicaCount;

    public StorageCluster(String etag, int dataReplicaCount, List<StorageNode> nodes) {
        this.etag = etag;
        this.dataReplicaCount = dataReplicaCount;
        int serverReplicas = 128;

        Map<String, StorageNode> nodeMap = Maps.newHashMap();
        for (StorageNode node : nodes) {
            nodeMap.put(node.getKey(), node);
        }
        ring = new ConsistentHash<>(serverReplicas, nodeMap);
    }

    public String getEtag() {
        return etag;
    }

    public int getDataReplicaCount() {
        return dataReplicaCount;
    }

    // public void add(StorageNode node) {
    // ring.add(node.getKey(), node);
    // }
    //
    // public void remove(StorageNode node) {
    // ring.remove(node.getKey(), node);
    // }
}
