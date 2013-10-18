package io.fathom.cloud.compute.state;

import io.fathom.cloud.protobuf.CloudModel.HostData;
import io.fathom.cloud.protobuf.CloudModel.HostGroupData;
import io.fathom.cloud.state.NumberedItemCollection;
import io.fathom.cloud.state.RepositoryBase;
import io.fathom.cloud.state.StateStore.StateNode;

import javax.inject.Singleton;

@Singleton
public class HostStore extends RepositoryBase {

    // public NumberedItemCollection<HostInfo> getHosts() {
    // StateNode root = stateStore.getRoot("computehosts");
    //
    // return new NumberedItemCollection<HostInfo>(root, HostInfo.newBuilder(),
    // HostInfo.getDescriptor()
    // .findFieldByNumber(HostInfo.ID_FIELD_NUMBER));
    // }

    public NumberedItemCollection<HostGroupData> getHostGroups() {
        StateNode root = stateStore.getRoot("netmap");
        StateNode groups = root.child("groups");

        return new NumberedItemCollection<HostGroupData>(groups, HostGroupData.newBuilder(), HostGroupData
                .getDescriptor().findFieldByNumber(HostGroupData.ID_FIELD_NUMBER));
    }

    public NumberedItemCollection<HostData> getHosts() {
        StateNode root = stateStore.getRoot("netmap");
        StateNode hosts = root.child("hosts");

        return new NumberedItemCollection<HostData>(hosts, HostData.newBuilder(), HostData.getDescriptor()
                .findFieldByNumber(HostData.ID_FIELD_NUMBER));
    }

}
