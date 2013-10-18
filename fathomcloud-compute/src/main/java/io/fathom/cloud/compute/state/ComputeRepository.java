package io.fathom.cloud.compute.state;

import io.fathom.cloud.protobuf.CloudModel.FlavorData;
import io.fathom.cloud.protobuf.CloudModel.InstanceData;
import io.fathom.cloud.protobuf.CloudModel.KeyPairData;
import io.fathom.cloud.protobuf.CloudModel.NetworkAddressData;
import io.fathom.cloud.protobuf.CloudModel.ReservationData;
import io.fathom.cloud.protobuf.CloudModel.SecurityGroupData;
import io.fathom.cloud.protobuf.CloudModel.VirtualIpData;
import io.fathom.cloud.protobuf.CloudModel.VirtualIpPoolData;
import io.fathom.cloud.server.model.Project;
import io.fathom.cloud.state.IdProvider;
import io.fathom.cloud.state.NamedItemCollection;
import io.fathom.cloud.state.NumberedItemCollection;
import io.fathom.cloud.state.RepositoryBase;
import io.fathom.cloud.state.StateStore.StateNode;
import io.fathom.cloud.state.StateStoreException;

import java.util.List;

import javax.inject.Singleton;

import com.google.common.collect.Lists;

@Singleton
public class ComputeRepository extends RepositoryBase {

    public NumberedItemCollection<ReservationData> getReservations(Project project) {
        StateNode root = stateStore.getRoot("reservations");
        StateNode projectNode = root.child(Long.toHexString(project.getId()));

        return new NumberedItemCollection<ReservationData>(projectNode, ReservationData.newBuilder(), ReservationData
                .getDescriptor().findFieldByNumber(ReservationData.ID_FIELD_NUMBER));
    }

    public NumberedItemCollection<InstanceData> getInstances(long projectId) {
        StateNode root = stateStore.getRoot("instances");
        StateNode node = root.child(Long.toHexString(projectId));

        return new NumberedItemCollection<InstanceData>(node, InstanceData.newBuilder(), InstanceData.getDescriptor()
                .findFieldByNumber(InstanceData.ID_FIELD_NUMBER));
    }

    public NamedItemCollection<VirtualIpData> getAllocatedVips(long poolId) {
        StateNode root = stateStore.getRoot("vips");
        StateNode pool = root.child(Long.toHexString(poolId));

        return new NamedItemCollection<VirtualIpData>(pool, VirtualIpData.newBuilder(), VirtualIpData.getDescriptor()
                .findFieldByNumber(VirtualIpData.IP_FIELD_NUMBER));
    }

    public NamedItemCollection<NetworkAddressData> getHostIps(long hostId, String networkKey) {
        StateNode ipsNode = stateStore.getRoot("ips");
        StateNode hostNode = ipsNode.child(Long.toHexString(hostId));
        StateNode poolNode = hostNode.child(networkKey);
        return new NamedItemCollection<NetworkAddressData>(poolNode, NetworkAddressData.newBuilder(),
                NetworkAddressData.getDescriptor().findFieldByNumber(NetworkAddressData.IP_FIELD_NUMBER));
    }

    public NumberedItemCollection<VirtualIpPoolData> getVirtualIpPools() {
        StateNode root = stateStore.getRoot("vippools");

        return new NumberedItemCollection<VirtualIpPoolData>(root, VirtualIpPoolData.newBuilder(), VirtualIpPoolData
                .getDescriptor().findFieldByNumber(VirtualIpPoolData.ID_FIELD_NUMBER));
    }

    public List<Long> listInstanceProjects() throws StateStoreException {
        StateNode root = stateStore.getRoot("instances");

        List<Long> ids = Lists.newArrayList();
        for (String key : root.getChildrenKeys()) {
            ids.add(Long.valueOf(key, 16));
        }
        return ids;
    }

    public NumberedItemCollection<SecurityGroupData> getSecurityGroups(long projectId) {
        StateNode root = stateStore.getRoot("securitygroups");
        StateNode node = root.child(Long.toHexString(projectId));

        return new NumberedItemCollection<SecurityGroupData>(node, SecurityGroupData.newBuilder(), SecurityGroupData
                .getDescriptor().findFieldByNumber(SecurityGroupData.ID_FIELD_NUMBER));
    }

    public NamedItemCollection<KeyPairData> getKeypairs(long projectId) {
        StateNode root = stateStore.getRoot("keys");
        StateNode node = root.child(Long.toHexString(projectId));

        return new NamedItemCollection<KeyPairData>(node, KeyPairData.newBuilder(), KeyPairData.getDescriptor()
                .findFieldByNumber(KeyPairData.KEY_FIELD_NUMBER));
    }

    public IdProvider getSecurityGroupRuleIdProvider() {
        return stateStore.getIdProvider("securitygrouprules.id");
    }

    public NumberedItemCollection<FlavorData> getFlavors() {
        StateNode node = stateStore.getRoot("flavors");

        return new NumberedItemCollection<FlavorData>(node, FlavorData.newBuilder(), FlavorData.getDescriptor()
                .findFieldByNumber(FlavorData.ID_FIELD_NUMBER));
    }

}
