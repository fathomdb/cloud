package io.fathom.cloud.cluster;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.protobuf.CloudCommons.NodeData;
import io.fathom.cloud.protobuf.CloudCommons.NodeType;
import io.fathom.cloud.state.DuplicateValueException;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.collect.Lists;

@Singleton
public class ClusterServiceImpl implements ClusterService {

    @Inject
    ClusterRepository clusterRepository;

    @Override
    public List<Node> findNodes(NodeType nodeType) throws CloudException {
        List<Node> nodes = Lists.newArrayList();

        for (NodeData node : clusterRepository.getNodes().list()) {
            if (node.getNodeType() != nodeType) {
                continue;
            }
            nodes.add(new NodeImpl(node));
        }
        return nodes;
    }

    @Override
    public void register(NodeData proposed) throws CloudException {
        String key = proposed.getKey();

        NodeData current = clusterRepository.getNodes().find(key);
        if (current != null && current.equals(proposed)) {
            return;
        }
        if (current == null) {
            try {
                NodeData.Builder b = NodeData.newBuilder(proposed);
                clusterRepository.getNodes().create(b);
            } catch (DuplicateValueException e) {
                throw new IllegalStateException("Concurrent update; duplicate host?", e);
            }
        } else {
            NodeData.Builder b = NodeData.newBuilder(proposed);
            clusterRepository.getNodes().update(b);
        }
    }

    public static class NodeImpl implements Node {

        private final NodeData data;

        public NodeImpl(NodeData data) {
            this.data = data;
        }

        @Override
        public String getKey() {
            return data.getKey();
        }

        @Override
        public List<String> getAddressList() {
            return data.getAddressList();
        }

        @Override
        public String getStore() {
            return data.getStore();
        }

        @Override
        public String getQueue() {
            return data.getQueue();
        }

    }

    @Override
    public String getEtag() throws CloudException {
        return clusterRepository.getNodes().getEtag();
    }
}
