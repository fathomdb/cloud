package io.fathom.cloud.blobs.replicated;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.blobs.sftp.SftpBlobStore;
import io.fathom.cloud.cluster.ClusterService;
import io.fathom.cloud.mq.MessageQueueService;
import io.fathom.cloud.mq.MessageQueueWriter;
import io.fathom.cloud.mq.QueuedRequestExecutor;
import io.fathom.cloud.protobuf.CloudCommons.NodeType;
import io.fathom.cloud.sftp.RemoteFile;
import io.fathom.cloud.ssh.SshConfig;
import io.fathom.cloud.ssh.SshContext;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathomdb.Configuration;
import com.google.common.collect.Lists;
import com.google.common.net.InetAddresses;
import com.google.inject.persist.Transactional;

@Transactional
public class StorageClusterBuilder {

    private static final Logger log = LoggerFactory.getLogger(StorageClusterBuilder.class);

    @Inject
    ClusterService clusterService;

    @Inject
    SshContext sshContext;

    @Inject
    Configuration config;

    @Inject
    MessageQueueService messageQueues;

    public StorageCluster build(int dataReplicaCount, String ifModifiedSince) throws IOException, CloudException {
        File localCacheDir = config.lookupFile("objectstore.cachedir", "/var/openstack/objectstore/cache");

        // Note that we get the etag first...
        // Any concurrent changes may be in the new version
        // i.e. we may end up doing a spurious rebuild
        String etag = clusterService.getEtag();

        if (ifModifiedSince != null && ifModifiedSince.equals(etag)) {
            return null;
        }

        List<StorageNode> nodes = Lists.newArrayList();

        for (ClusterService.Node nodeData : clusterService.findNodes(NodeType.STORAGE)) {
            String key = nodeData.getKey();

            // TODO: Don't use sftp for self-connections!!

            InetAddress bestAddress = null;

            for (String s : nodeData.getAddressList()) {
                InetAddress address = InetAddresses.forString(s);
                if (bestAddress == null) {
                    bestAddress = address;
                } else {
                    log.warn("Comparison between node addresses not implemented: {} vs {}", bestAddress, address);
                }
            }

            if (bestAddress == null) {
                throw new IllegalStateException("Unable to find suitable address to communicate with node: " + nodeData);
            }

            InetSocketAddress sshSocketAddress = new InetSocketAddress(bestAddress, 22);
            SshConfig sshConfig = sshContext.buildConfig(sshSocketAddress);

            SftpBlobStore.Factory blobStoreFactory;
            String store = nodeData.getStore();
            if (store.startsWith("sftp://")) {
                File path = new File(store.substring(7));
                blobStoreFactory = new SftpBlobStore.Factory(sshConfig, new RemoteFile(path), localCacheDir);
            } else {
                throw new IllegalArgumentException();
            }

            String queue = nodeData.getQueue();
            MessageQueueWriter mqWriter = messageQueues.getWriter(sshConfig, queue);

            nodes.add(new StorageNode(key, blobStoreFactory, new QueuedRequestExecutor(mqWriter)));
        }

        return new StorageCluster(etag, dataReplicaCount, nodes);
    }

    public StorageCluster build() throws IOException, CloudException {
        int dataReplicaCount = getConfiguredDataReplicaCount();
        return build(dataReplicaCount, null);
    }

    public int getConfiguredDataReplicaCount() {
        return config.lookup("objectstore.replicas", 1);
    }
}
