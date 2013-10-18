package io.fathom.cloud.blobs.replicated;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClusterState {
    private static final Logger log = LoggerFactory.getLogger(ClusterState.class);

    final StorageClusterBuilder builder;
    private StorageCluster cluster;

    @Inject
    public ClusterState(StorageClusterBuilder builder) {
        super();
        this.builder = builder;
    }

    public synchronized StorageCluster getCluster() {
        if (this.cluster == null) {
            try {
                this.cluster = builder.build();
            } catch (Exception e) {
                log.warn("Unable to initialize cluster", e);
                throw new IllegalStateException("Unable to initialize cluster", e);
            }
        }
        return cluster;
    }

    public StorageClusterBuilder getStorageClusterBuilder() {
        return builder;
    }

    public synchronized void setCluster(StorageCluster cluster) {
        this.cluster = cluster;
    }
}
