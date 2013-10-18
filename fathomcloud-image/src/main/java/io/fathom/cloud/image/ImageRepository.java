package io.fathom.cloud.image;

import io.fathom.cloud.protobuf.ImageModel.ImageData;
import io.fathom.cloud.state.NumberedItemCollection;
import io.fathom.cloud.state.RepositoryBase;
import io.fathom.cloud.state.StateStore.StateNode;

import javax.inject.Singleton;

@Singleton
public class ImageRepository extends RepositoryBase {

    public NumberedItemCollection<ImageData> getImages() {
        StateNode root = stateStore.getRoot("images");

        return new NumberedItemCollection<ImageData>(root, ImageData.newBuilder(), ImageData.getDescriptor()
                .findFieldByNumber(ImageData.ID_FIELD_NUMBER));
    }

    // public NumberedItemCollection<InstanceInfo> getInstances(long projectId)
    // {
    // StateNode root = stateStore.getRoot("instances");
    // StateNode node = root.child(Long.toHexString(projectId));
    //
    // return new NumberedItemCollection<InstanceInfo>(node,
    // InstanceInfo.newBuilder(), InstanceInfo.getDescriptor()
    // .findFieldByNumber(InstanceInfo.ID_FIELD_NUMBER));
    // }
    //
    // public NumberedItemCollection<SecurityGroupData> getSecurityGroups(
    // long projectId) {
    // StateNode root = stateStore.getRoot("securitygroups");
    // StateNode node = root.child(Long.toHexString(projectId));
    //
    // return new NumberedItemCollection<SecurityGroupData>(node,
    // SecurityGroupData.newBuilder(), SecurityGroupData
    // .getDescriptor().findFieldByNumber(
    // SecurityGroupData.ID_FIELD_NUMBER));
    // }
    //
    // public NamedItemCollection<KeyPairData> getKeypairs(long projectId) {
    // StateNode root = stateStore.getRoot("keys");
    // StateNode node = root.child(Long.toHexString(projectId));
    //
    // return new NamedItemCollection<KeyPairData>(node,
    // KeyPairData.newBuilder(), KeyPairData.getDescriptor()
    // .findFieldByNumber(KeyPairData.KEY_FIELD_NUMBER));
    // }
    //
    // public IdProvider getSecurityGroupRuleIdProvider() {
    // return stateStore.getIdProvider("securitygrouprules.id");
    // }
}
