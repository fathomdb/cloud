package io.fathom.cloud.storage;

import io.fathom.cloud.blobs.BlobData;

import java.io.File;
import java.io.IOException;

import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import com.google.protobuf.ByteString;

public class FileBlob {
    public final ByteString hash;
    public final ByteString hashResume;
    public final long dataLength;
    public final BlobData data;

    public FileBlob(ByteString hash, ByteString hashResume, long dataLength, BlobData data) {
        this.hash = hash;
        this.hashResume = hashResume;
        this.dataLength = dataLength;
        this.data = data;
    }

    public static FileBlob build(File src) throws IOException {
        return build(Files.asByteSource(src));
    }

    public static FileBlob build(ByteSource src) throws IOException {
        long dataLength = src.size();

        // We'd probably do better with a bigger hash,
        // but we need this for the etag
        // HashCode md5 = Hashing.md5().hashBytes(data);
        ResumableMD5Digest md5 = ResumableMD5Digest.get();
        md5.update(src);

        ByteString hashResume = md5.getState();
        ByteString hash = ByteString.copyFrom(md5.digest());

        BlobData blob = new BlobData(src, hash);
        return new FileBlob(hash, hashResume, dataLength, blob);
    }

}
