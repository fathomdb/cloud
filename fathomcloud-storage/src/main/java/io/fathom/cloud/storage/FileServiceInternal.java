package io.fathom.cloud.storage;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.blobs.BlobStore;
import io.fathom.cloud.protobuf.FileModel.BucketAttributes;
import io.fathom.cloud.server.model.Project;
import io.fathom.cloud.server.model.User;
import io.fathom.cloud.storage.FileService;
import io.fathom.cloud.storage.api.os.resources.DirectoryListEntry;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

import com.google.common.util.concurrent.ListenableFuture;

public interface FileServiceInternal extends FileService {

    FsFile findFileInfo(FsBucket bucket, String name) throws CloudException;

    void deleteBucket(Project project, String bucketName) throws CloudException;

    Status putBucket(Project project, String bucketName, BucketAttributes bucketAttributes,
            Map<String, String> userAttributes) throws CloudException;

    FsBucket findBucket(User user, Project project, String bucketName) throws CloudException;

    StreamingOutput open(FsFile file, Long from, Long to);

    BlobStore getBlobStore(Project project) throws IOException;

    boolean compact(CompactOperation compaction) throws IOException, CloudException;

    ListenableFuture<?> watchBucket(FsBucket bucket, String since) throws CloudException;

    // List<DirectoryListEntry> listFiles(Project project, FsBucket bucket)
    // throws CloudException;
    List<DirectoryListEntry> listFiles(Project project, FsBucket bucket, String prefix, String delimiter, String marker)
            throws CloudException;

}
