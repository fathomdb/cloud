package io.fathom.cloud.openstack.client.storage.model;

import java.util.Date;

import com.google.gson.annotations.SerializedName;

public class StorageObjectInfo {
    // If this is a subdir...
    // TODO: This sort of sucks
    public String subdir;

    public String name;
    public String hash;

    @SerializedName("bytes")
    public long length;

    @SerializedName("content_type")
    public String contentType;

    @SerializedName("last_modified")
    public Date lastModified;

    public long getContentLength() {
        return length;
    }

    public Date getLastModifiedDate() {
        return lastModified;
    }

    public String getKey() {
        return name;
    }

    public long getLastModifiedTimestamp() {
        if (lastModified == null) {
            return 0;
        }
        return lastModified.getTime();
    }

    public boolean isSubdir() {
        return subdir != null;
    }

}
