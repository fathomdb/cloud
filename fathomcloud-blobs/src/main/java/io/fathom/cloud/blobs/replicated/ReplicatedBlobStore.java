package io.fathom.cloud.blobs.replicated;

import io.fathom.cloud.blobs.BlobData;
import io.fathom.cloud.blobs.BlobStore;
import io.fathom.cloud.blobs.BlobStoreBase;
import io.fathom.cloud.blobs.BlobStoreFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathomdb.utils.Hex;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.google.protobuf.ByteString;

public class ReplicatedBlobStore extends BlobStoreBase {
    private static final Logger log = LoggerFactory.getLogger(ReplicatedBlobStore.class);

    public static class Factory implements BlobStoreFactory {
        final ClusterState clusterState;

        @Inject
        public Factory(ClusterState clusterState) {
            this.clusterState = clusterState;
        }

        @Override
        public BlobStore get(String key) throws IOException {
            Preconditions.checkArgument(!Strings.isNullOrEmpty(key));

            return new ReplicatedBlobStore(clusterState, key);
        }

        public ClusterState getClusterState() {
            return clusterState;
        }
    }

    final ClusterState clusterState;
    private final String blobStoreKey;

    // public ReplicatedBlobStore(StorageCluster cluster) {
    // super();
    // this.cluster = cluster;
    // }

    @Inject
    public ReplicatedBlobStore(ClusterState cluster, String blobStoreKey) {
        this.clusterState = cluster;
        this.blobStoreKey = blobStoreKey;
    }

    @Override
    public BlobData find(ByteString key) throws IOException {
        StorageCluster cluster = getCluster();
        Iterator<StorageNode> nodes = cluster.ring.walkRing(key);

        Set<StorageNode> checked = Sets.newHashSet();

        // TODO: Run in parallel?
        while (nodes.hasNext()) {
            StorageNode node = nodes.next();

            if (checked.contains(node)) {
                continue;
            }

            try {
                BlobData is = node.getBlobStore(blobStoreKey).find(key);
                if (is != null) {
                    log.debug("Found blob {} on node {}", Hex.toHex(key.toByteArray()), node.getKey());
                    return is;
                }
            } catch (IOException e) {
                log.warn("Failed to read from node " + node, e);
            }
            checked.add(node);
        }

        // TODO: Hopefully we only hit this path in error
        log.warn("Unable to find blob (after checking every node): {}", key);

        return null;
    }

    @Override
    public boolean has(ByteString key, boolean checkCache) throws IOException {
        StorageCluster cluster = getCluster();
        Iterator<StorageNode> nodes = cluster.ring.walkRing(key);

        Set<StorageNode> checked = Sets.newHashSet();

        // TODO: Run in parallel?
        while (nodes.hasNext()) {
            StorageNode node = nodes.next();
            if (checked.contains(node)) {
                continue;
            }

            try {
                boolean has = node.getBlobStore(blobStoreKey).has(key, checkCache);
                if (has) {
                    return true;
                }
            } catch (IOException e) {
                log.warn("Failed to read from node " + node, e);
            }
            checked.add(node);
        }

        // TODO: Hopefully we only hit this path in error
        log.warn("Unable to find blob (after checking every node): {}", key);

        return false;
    }

    @Override
    public void put(BlobData data) throws IOException {
        ByteString key = data.getHash();

        StorageCluster cluster = getCluster();
        Iterator<StorageNode> nodes = cluster.ring.walkRing(key);

        Set<StorageNode> yes = Sets.newHashSet();
        Set<StorageNode> no = Sets.newHashSet();

        // TODO: Run in parallel?
        while (nodes.hasNext()) {
            StorageNode node = nodes.next();

            if (yes.contains(node)) {
                continue;
            }

            if (no.contains(node)) {
                continue;
            }

            try {
                node.getBlobStore(blobStoreKey).put(data);
                yes.add(node);

                if (yes.size() >= cluster.dataReplicaCount) {
                    return;
                }
            } catch (IOException e) {
                log.warn("Failed to add to node " + node, e);
                no.add(node);
            }
        }

        throw new IOException("Unable to save to sufficient nodes (reached " + yes.size() + "/"
                + cluster.dataReplicaCount + ")");
    }

    @Override
    public Iterable<ByteString> listWithPrefix(String prefix) throws IOException {
        throw new UnsupportedOperationException();
    }

    public StorageCluster getCluster() {
        return clusterState.getCluster();
    }

}
