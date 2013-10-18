package io.fathom.cloud.cluster;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.protobuf.CloudCommons.NodeData;
import io.fathom.cloud.protobuf.CloudCommons.NodeType;

import java.util.List;

import com.google.inject.ImplementedBy;

@ImplementedBy(ClusterServiceImpl.class)
public interface ClusterService {

    public interface Node {

        String getKey();

        List<String> getAddressList();

        String getStore();

        String getQueue();

    }

    List<Node> findNodes(NodeType storage) throws CloudException;

    void register(NodeData proposed) throws CloudException;

    String getEtag() throws CloudException;

}
