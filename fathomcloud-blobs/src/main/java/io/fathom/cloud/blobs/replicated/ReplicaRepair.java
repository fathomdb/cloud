package io.fathom.cloud.blobs.replicated;

import io.fathom.cloud.blobs.BlobData;
import io.fathom.cloud.blobs.BlobStore;
import io.fathom.cloud.protobuf.CloudCommons.FixReplica;
import io.fathom.cloud.protobuf.CloudCommons.PeerRequest;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathomdb.TimeSpan;
import com.fathomdb.utils.Hex;
import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.protobuf.ByteString;

public class ReplicaRepair {

    private static final Logger log = LoggerFactory.getLogger(ReplicaRepair.class);

    final StorageCluster cluster;

    final String blobStoreKey;

    public ReplicaRepair(StorageCluster cluster, String blobStoreKey) {
        super();
        this.cluster = cluster;
        this.blobStoreKey = blobStoreKey;
    }

    final Map<StorageNode, PeerRequest.Builder> peerRequests = Maps.newHashMap();

    PeerRequest.Builder getPeerRequest(StorageNode node) {
        PeerRequest.Builder peerRequest = peerRequests.get(node);
        if (peerRequest == null) {
            peerRequest = PeerRequest.newBuilder();
            peerRequests.put(node, peerRequest);
        }
        return peerRequest;
    }

    public void repair(String prefix) {
        HashMultimap<ByteString, StorageNode> keyMap = HashMultimap.create();

        {
            List<StorageNode> queue = Lists.newArrayList(cluster.ring.all());
            // TODO: Run in parallel?
            for (int attempt = 1; attempt <= 3; attempt++) {
                List<StorageNode> retry = Lists.newArrayList();
                for (StorageNode node : queue) {
                    try {
                        Iterable<ByteString> keys = node.getBlobStore(blobStoreKey).listWithPrefix(prefix);

                        // TODO: intern strings
                        // TODO: use a smarter data structure??
                        // TODO: These aren't really strings; they're actually
                        // hex
                        // of MD5s

                        for (ByteString key : keys) {
                            keyMap.put(key, node);
                        }
                    } catch (IOException e) {
                        log.warn("Failed to list storage node " + node, e);
                        retry.add(node);
                    }
                }

                queue = retry;
                if (queue.isEmpty()) {
                    break;
                }

                TimeSpan.FIVE_SECONDS.doSafeSleep();
            }

            if (!queue.isEmpty()) {
                log.warn("Some storage nodes were not reachable; treating as down: " + Joiner.on(";").join(queue));
            }
        }

        // TODO: Throttle replication traffic

        // -------------------------------------------------
        // Copy any blobs without sufficient replicas
        // -------------------------------------------------
        // We copy anything with 1 replica first, etc...
        for (int i = 1; i < cluster.dataReplicaCount; i++) {
            for (ByteString key : keyMap.keySet()) {
                Set<StorageNode> nodes = keyMap.get(key);
                if (nodes.size() != i) {
                    continue;
                }

                log.info("Node under-replicated: {} count={}", forDebug(key), i);

                Set<StorageNode> replicated = replicate(key, nodes);
                if (replicated.size() < cluster.dataReplicaCount) {
                    log.warn("Unable to copy to sufficient replicas: {}", key);
                }

                // TODO: Update map??
            }
        }

        // -------------------------------------------------
        // Check out any over-replicated blobs
        // -------------------------------------------------
        // TODO: Drop thread priority now?
        for (ByteString key : keyMap.keySet()) {
            Set<StorageNode> nodes = keyMap.get(key);
            if (nodes.size() <= cluster.dataReplicaCount) {
                continue;
            }

            log.warn("Node over-replicated: {} count={}", key);

            Set<StorageNode> correct = Sets.newHashSet();
            Set<StorageNode> shouldRemove = Sets.newHashSet();
            Set<StorageNode> shouldAdd = Sets.newHashSet();

            Iterator<StorageNode> walkRing = cluster.ring.walkRing(key);
            while (walkRing.hasNext()) {
                StorageNode node = walkRing.next();
                if (nodes.contains(node)) {
                    correct.add(node);
                } else {
                    shouldAdd.add(node);
                }

                if ((correct.size() + shouldAdd.size()) > cluster.dataReplicaCount) {
                    break;
                }
            }

            for (StorageNode node : nodes) {
                if (!correct.contains(node)) {
                    shouldRemove.add(node);
                }
            }

            // TODO: Remove by moving to cache
            // TODO: Don't move stuff around if it's a node that's down...
            log.warn("Should remove: {} add: {}", Joiner.on(";").join(shouldRemove), Joiner.on(";").join(shouldAdd));
        }

        // -------------------------------------------------
        // Find blobs on the wrong nodes
        // -------------------------------------------------
        // TODO: We don't do this now; we rely on
        log.warn("Blob moving to correct nodes is not implemented");

        flushPeerRequests();
    }

    private void flushPeerRequests() {
        for (Entry<StorageNode, PeerRequest.Builder> entry : peerRequests.entrySet()) {
            StorageNode node = entry.getKey();
            PeerRequest.Builder prb = entry.getValue();

            PeerRequest pr = prb.build();

            try {
                node.requestExecutor.execute(pr.toByteArray());
            } catch (IOException e) {
                log.error("Error enqueuing peer request", e);
            }
        }
        peerRequests.clear();
    }

    private String forDebug(ByteString key) {
        return Hex.toHex(key.toByteArray());
    }

    private Set<StorageNode> replicate(ByteString key, Set<StorageNode> nodes) {
        Iterator<StorageNode> walkRing = cluster.ring.walkRing(key);

        Set<StorageNode> yes = Sets.newHashSet(nodes);
        Set<StorageNode> no = Sets.newHashSet();

        // TODO: Run in parallel?
        while (walkRing.hasNext()) {
            StorageNode node = walkRing.next();

            if (yes.contains(node)) {
                continue;
            }

            if (no.contains(node)) {
                continue;
            }

            try {
                copy(key, yes, node);
                yes.add(node);

                if (yes.size() >= cluster.dataReplicaCount) {
                    return yes;
                }
            } catch (IOException e) {
                log.warn("Failed to copy to node " + node, e);
                no.add(node);
            }
        }

        return yes;
    }

    private void copy(ByteString key, Set<StorageNode> src, StorageNode dest) throws IOException {
        PeerRequest.Builder peerRequest = getPeerRequest(dest);
        FixReplica.Builder frb = peerRequest.addFixReplicaBuilder();
        frb.setBlobStoreKey(blobStoreKey);
        frb.addBlobKey(key);
    }

    public void fixReplicate(BlobStore global, BlobStore local, ByteString key) throws IOException {
        if (local.has(key, false)) {
            return;
        }

        Iterator<StorageNode> walkRing = cluster.ring.walkRing(key);

        // Regardless of what the state was when the request was sent, if the
        // first N nodes now have the blob, then don't replicate

        // TODO: Check first if we're one of the N nodes; if so, just copy if we
        // don't have it

        // TODO: If any of the N nodes don't have it, early-exit the loop
        Set<StorageNode> yes = Sets.newHashSet();
        Set<StorageNode> no = Sets.newHashSet();

        // TODO: Run in parallel?
        while (walkRing.hasNext()) {
            StorageNode node = walkRing.next();

            if (yes.contains(node)) {
                continue;
            }

            if (no.contains(node)) {
                continue;
            }

            try {
                if (node.getBlobStore(blobStoreKey).has(key, false)) {
                    yes.add(node);
                } else {
                    no.add(node);
                }
            } catch (IOException e) {
                log.warn("Error communicating with node " + node, e);
                no.add(node);
            }

            if ((yes.size() + no.size()) >= cluster.dataReplicaCount) {
                break;
            }
        }

        if (yes.size() >= cluster.dataReplicaCount) {
            return;
        }

        BlobData data = null;
        try {
            for (StorageNode node : yes) {
                try {
                    data = node.getBlobStore(blobStoreKey).find(key);
                    break;
                } catch (IOException e) {
                    log.warn("Error communicating with node " + node, e);
                }
            }

            if (data == null) {
                data = global.find(key);
            }

            if (data == null) {
                log.error("Unable to find blob: {}", forDebug(key));
                return;
            }

            // TODO: This won't scale to big data sizes..
            log.info("Writing replica on {}: {}", local, forDebug(key));
            local.put(data);
        } finally {
            if (data != null) {
                data.close();
            }
        }
    }
}
