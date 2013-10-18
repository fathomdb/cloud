package io.fathom.cloud.storage;

import io.fathom.cloud.protobuf.CloudCommons.Attributes;
import io.fathom.cloud.protobuf.CloudCommons.KeyValueData;
import io.fathom.cloud.protobuf.FileModel.BucketData;
import io.fathom.cloud.server.model.Project;

import com.google.common.base.Optional;

public class FsBucket {
    final Project project;
    final BucketData data;

    Optional<String> metaWebIndex;

    FsBucket(Project project, BucketData bucket) {
        this.project = project;
        this.data = bucket;
    }

    public BucketData getData() {
        return data;
    }

    public Project getProject() {
        return project;
    }

    public String getMetaWebIndex() {
        if (metaWebIndex == null) {
            metaWebIndex = findMeta("web-index");
        }
        return metaWebIndex.orNull();
    }

    private Optional<String> findMeta(String key) {
        key = key.toLowerCase();

        Attributes attributes = data.getAttributes();
        for (KeyValueData kv : attributes.getUserAttributesList()) {
            if (key.equals(kv.getKey())) {
                return Optional.of(kv.getValue());
            }
        }
        return Optional.absent();
    }
}
