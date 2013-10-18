package io.fathom.cloud.storage.api.os.resources;

import io.fathom.cloud.protobuf.FileModel.FileData;
import io.fathom.cloud.storage.FileService.FileInfo;

public class DirectoryListEntry implements Comparable<DirectoryListEntry>, FileInfo {
    final String key;
    final FileData file;

    public DirectoryListEntry(String key, FileData file) {
        this.key = key;
        this.file = file;
    }

    public String getKey() {
        return key;
    }

    public FileData getFile() {
        return file;
    }

    @Override
    public boolean isDirectory() {
        return file == null;
    }

    @Override
    public int compareTo(DirectoryListEntry o) {
        return key.compareTo(o.key);
    }

    @Override
    public long getLength() {
        return file.getLength();
    }

    @Override
    public String getPath() {
        return file.getKey();
    }

}
