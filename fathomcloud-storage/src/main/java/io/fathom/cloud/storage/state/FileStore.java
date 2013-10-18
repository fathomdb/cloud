package io.fathom.cloud.storage.state;

import io.fathom.cloud.protobuf.FileModel.BucketData;
import io.fathom.cloud.protobuf.FileModel.DirectoryData;
import io.fathom.cloud.state.NamedItemCollection;
import io.fathom.cloud.state.NumberedItemCollection;
import io.fathom.cloud.state.RepositoryBase;
import io.fathom.cloud.state.StateStore.StateNode;

import javax.inject.Singleton;

@Singleton
public class FileStore extends RepositoryBase {
    public NamedItemCollection<BucketData> getBuckets(long projectId) {
        StateNode root = stateStore.getRoot("bucket");
        StateNode node = root.child(Long.toHexString(projectId));

        return new NamedItemCollection<BucketData>(node, BucketData.newBuilder(), BucketData.getDescriptor()
                .findFieldByNumber(BucketData.KEY_FIELD_NUMBER));
    }

    public NumberedItemCollection<DirectoryData> getDirectories(long projectId) {
        StateNode root = stateStore.getRoot("dir");
        StateNode node = root.child(Long.toHexString(projectId));

        return new NumberedItemCollection<DirectoryData>(node, DirectoryData.newBuilder(), DirectoryData
                .getDescriptor().findFieldByNumber(DirectoryData.ID_FIELD_NUMBER));
    }

}
