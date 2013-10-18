package io.fathom.cloud.mq;

import io.fathom.cloud.blobs.BlobStore;
import io.fathom.cloud.blobs.replicated.ReplicaRepair;
import io.fathom.cloud.blobs.replicated.ReplicatedBlobStore;
import io.fathom.cloud.mq.RequestExecutor;
import io.fathom.cloud.protobuf.CloudCommons.FixReplica;
import io.fathom.cloud.protobuf.CloudCommons.PeerRequest;

import java.io.IOException;

import com.google.protobuf.ByteString;

public class LocalRequestExecutor implements RequestExecutor {

    final ReplicatedBlobStore replicatedBlobStore;
    final BlobStore localBlobStore;

    public LocalRequestExecutor(ReplicatedBlobStore replicatedBlobStore, BlobStore localBlobStore) {
        super();
        this.replicatedBlobStore = replicatedBlobStore;
        this.localBlobStore = localBlobStore;
    }

    @Override
    public void execute(byte[] bytes) throws IOException {
        // TODO: Use injection to make this cleaner??

        PeerRequest pr = PeerRequest.parseFrom(bytes);

        if (pr.getFixReplicaCount() != 0) {
            for (FixReplica fr : pr.getFixReplicaList()) {
                ReplicaRepair repair = new ReplicaRepair(replicatedBlobStore.getCluster(), fr.getBlobStoreKey());

                // TODO: Run in parallel??
                for (ByteString blobKey : fr.getBlobKeyList()) {
                    repair.fixReplicate(replicatedBlobStore, localBlobStore, blobKey);
                }
            }
        }
    }

}
