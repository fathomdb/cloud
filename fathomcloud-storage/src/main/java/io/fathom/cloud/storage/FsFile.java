package io.fathom.cloud.storage;

import io.fathom.cloud.protobuf.FileModel.FileData;
import io.fathom.cloud.server.model.Project;
import io.fathom.cloud.storage.FileService.FileInfo;

public class FsFile implements FileInfo {
    final FsBucket bucket;
    final FileData data;

    public FsFile(FsBucket bucket, FileData data) {
        this.bucket = bucket;
        this.data = data;
    }

    public FileData getData() {
        return data;
    }

    public FsBucket getBucket() {
        return bucket;
    }

    public Project getProject() {
        return bucket.getProject();
    }

    @Override
    public long getLength() {
        return data.getLength();
    }

    @Override
    public String getPath() {
        return bucket.getData().getKey();
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

}
