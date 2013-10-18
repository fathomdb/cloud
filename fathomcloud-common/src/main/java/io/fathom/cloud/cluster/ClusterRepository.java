package io.fathom.cloud.cluster;

import io.fathom.cloud.protobuf.CloudCommons.NodeData;
import io.fathom.cloud.state.NamedItemCollection;
import io.fathom.cloud.state.RepositoryBase;
import io.fathom.cloud.state.StateStore.StateNode;

import javax.inject.Singleton;

@Singleton
public class ClusterRepository extends RepositoryBase {
    public NamedItemCollection<NodeData> getNodes() {
        StateNode root = stateStore.getRoot("nodes");

        return new NamedItemCollection<NodeData>(root, NodeData.newBuilder(), NodeData.getDescriptor()
                .findFieldByNumber(NodeData.KEY_FIELD_NUMBER));
    }
}
