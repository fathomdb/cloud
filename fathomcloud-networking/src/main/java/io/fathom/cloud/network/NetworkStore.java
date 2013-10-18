package io.fathom.cloud.network;

import io.fathom.cloud.protobuf.NetworkingModel.NetworkData;
import io.fathom.cloud.protobuf.NetworkingModel.SubnetData;
import io.fathom.cloud.state.NumberedItemCollection;
import io.fathom.cloud.state.RepositoryBase;
import io.fathom.cloud.state.StateStore.StateNode;

import javax.inject.Singleton;

@Singleton
public class NetworkStore extends RepositoryBase {

    public NumberedItemCollection<NetworkData> getSharedNetworks() {
        StateNode root = stateStore.getRoot("networks");
        return new NumberedItemCollection<NetworkData>(root, NetworkData.newBuilder(), NetworkData.getDescriptor()
                .findFieldByNumber(NetworkData.ID_FIELD_NUMBER));
    }

    public NumberedItemCollection<SubnetData> getSharedSubnets() {
        StateNode root = stateStore.getRoot("subnets");
        return new NumberedItemCollection<SubnetData>(root, SubnetData.newBuilder(), SubnetData.getDescriptor()
                .findFieldByNumber(SubnetData.ID_FIELD_NUMBER));

    }

}
