package io.fathom.cloud.blobs.replicated;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.blobs.BlobStoreFactory;
import io.fathom.cloud.tasks.ScheduledTask;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathomdb.Configuration;
import com.fathomdb.TimeSpan;

@Singleton
public class UpdateClusterTask extends ScheduledTask {
    private static final Logger log = LoggerFactory.getLogger(UpdateClusterTask.class);

    @Inject
    BlobStoreFactory blobStoreFactory;

    @Inject
    StorageClusterBuilder storageClusterBuilder;

    @Inject
    Configuration config;

    @Override
    public synchronized void run() throws IOException, CloudException {
        int dataReplicaCount = storageClusterBuilder.getConfiguredDataReplicaCount();

        ReplicatedBlobStore.Factory replicatedBlobStore = (ReplicatedBlobStore.Factory) blobStoreFactory;

        String ifModifiedSince = null;

        ClusterState clusterState = replicatedBlobStore.getClusterState();
        StorageCluster current = clusterState.getCluster();
        if (current != null) {
            ifModifiedSince = current.getEtag();
            if (dataReplicaCount != current.getDataReplicaCount()) {
                ifModifiedSince = null;
            }
        }

        StorageCluster storageCluster = clusterState.getStorageClusterBuilder()
                .build(dataReplicaCount, ifModifiedSince);
        current = storageCluster;

        clusterState.setCluster(storageCluster);
    }

    @Override
    protected TimeSpan getInterval() {
        return TimeSpan.ONE_MINUTE;
    }
}
