package io.fathom.cloud.storage;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.protobuf.FileModel.BucketData;
import io.fathom.cloud.server.model.Project;

public interface StorageDerivedMetadata {

    void apply(Project project, BucketData bucket, String serviceKey) throws CloudException;

}
