package io.fathom.cloud.storage;

import io.fathom.cloud.protobuf.FileModel.BucketData;
import io.fathom.cloud.server.model.Project;

public class NullStorageDerivedMetadata implements StorageDerivedMetadata {

    @Override
    public void apply(Project project, BucketData bucket, String serviceKey) {

    }

}
