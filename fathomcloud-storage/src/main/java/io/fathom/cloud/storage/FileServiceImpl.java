package io.fathom.cloud.storage;

import io.fathom.cloud.Clock;
import io.fathom.cloud.CloudException;
import io.fathom.cloud.blobs.BlobData;
import io.fathom.cloud.blobs.BlobStore;
import io.fathom.cloud.blobs.BlobStoreFactory;
import io.fathom.cloud.io.HashingOutputStream;
import io.fathom.cloud.protobuf.CloudCommons.Attributes;
import io.fathom.cloud.protobuf.CloudCommons.KeyValueData;
import io.fathom.cloud.protobuf.FileModel.BucketAttributes;
import io.fathom.cloud.protobuf.FileModel.BucketData;
import io.fathom.cloud.protobuf.FileModel.DirectoryData;
import io.fathom.cloud.protobuf.FileModel.DirectoryDataOrBuilder;
import io.fathom.cloud.protobuf.FileModel.FileData;
import io.fathom.cloud.protobuf.FileModel.FileRange;
import io.fathom.cloud.server.model.Project;
import io.fathom.cloud.server.model.User;
import io.fathom.cloud.state.DuplicateValueException;
import io.fathom.cloud.state.NamedItemCollection;
import io.fathom.cloud.state.NumberedItemCollection;
import io.fathom.cloud.state.Watched;
import io.fathom.cloud.storage.FileBlob;
import io.fathom.cloud.storage.FilePutOption;
import io.fathom.cloud.storage.FileService;
import io.fathom.cloud.storage.ResumableMD5Digest;
import io.fathom.cloud.storage.api.os.models.CloudObject;
import io.fathom.cloud.storage.api.os.models.StorageAcl;
import io.fathom.cloud.storage.api.os.models.StorageAcl.AclType;
import io.fathom.cloud.storage.api.os.resources.DirectoryListEntry;
import io.fathom.cloud.storage.state.FileStore;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathomdb.utils.Hex;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
import com.google.common.io.ByteStreams;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.persist.Transactional;
import com.google.protobuf.ByteString;

@Singleton
@Transactional
public class FileServiceImpl implements FileService, FileServiceInternal {
    private static final Logger log = LoggerFactory.getLogger(FileServiceImpl.class);

    public static final int COMPACTION_THRESHOLD = 32;

    private static final boolean PARANOID = true;

    @Inject
    FileStore fileStore;

    @Inject
    BlobStoreFactory blobStoreFactory;

    @Inject
    FilesystemCompactor compactor;

    @Inject
    Provider<StorageDerivedMetadata> derivedMetadataProvider;

    @Override
    public BlobStore getBlobStore(Project project) throws IOException {
        String key = "obj_" + project.getId();
        return blobStoreFactory.get(key);
    }

    protected NumberedItemCollection<DirectoryData> getDirectoryStorage(Project project) throws CloudException {
        NumberedItemCollection<DirectoryData> directoryStorage = fileStore.getDirectories(project.getId());
        return directoryStorage;
    }

    BucketData getBucket(Project project, String bucketName) throws CloudException {
        NamedItemCollection<BucketData> bucketStorage = fileStore.getBuckets(project.getId());
        BucketData bucket = bucketStorage.find(bucketName);
        if (bucket == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }
        return bucket;
    }

    public DirectoryData storeDirectory(NumberedItemCollection<DirectoryData> directoryStorage,
            DirectoryData.Builder dir) throws CloudException {
        if (!dir.hasId()) {
            // A new directory
            // We expect this to be small
            DirectoryData created = directoryStorage.create(dir);

            assert created.getSerializedSize() < 1024;

            return created;
        }

        DirectoryData built = dir.build();

        int size = built.getSerializedSize();
        if (size > 4096) {
            // log.warn("Excessive size of DirectoryData.  Size={} Data={}",
            // size,
            // built);
            log.warn("Excessive size of DirectoryData.  Size={}", size);
        }
        return directoryStorage.update(dir);
    }

    private DirectoryData getDirectoryData(Project project, BucketData bucket) throws CloudException {
        NumberedItemCollection<DirectoryData> directoryStorage = getDirectoryStorage(project);
        DirectoryData dir = directoryStorage.find(bucket.getRootId());
        if (dir == null) {
            throw new IllegalStateException();
        }
        return dir;
    }

    @Override
    @Transactional
    public void putFile(Project project, String bucketName, String name, FileBlob blob, String contentType,
            Map<String, String> userAttributes, FilePutOption... options) throws CloudException, IOException {

        // Otherwise this causes all sorts of problems...
        if (name.startsWith("/")) {
            name = name.substring(1);
        }

        // TODO: Should we use the straight hash as the key?
        // Should we include a second hash?
        // Should we prefix the project id?
        getBlobStore(project).put(blob.data);

        BucketData bucket = getBucket(project, bucketName);

        DirectoryData dir = getDirectoryData(project, bucket);

        DirectoryData.Builder newDir = DirectoryData.newBuilder(dir);

        FileData.Builder file = null;

        int foundIndex = findFile(newDir, name);
        if (foundIndex != -1) {
            for (FilePutOption option : options) {
                if (option == FilePutOption.FAIL_IF_EXISTS) {
                    throw new IOException("File already exists: " + name);
                }
            }
            file = newDir.getFilesBuilder(foundIndex);
            file.clearRanges();
        } else {
            file = newDir.addFilesBuilder();
            file.setKey(name);
        }

        file.setLastModified(System.currentTimeMillis());

        if (contentType != null) {
            file.setContentType(contentType);
        }

        file.setHash(blob.hash);

        // NOTE: hashResume can contain plaintext. Careful around encryption!
        file.setHashResume(blob.hashResume);

        {
            FileRange.Builder c = file.addRangesBuilder();
            c.setStart(0);
            c.setEnd(blob.dataLength);
            c.setContentKey(blob.hash);
        }

        // builder.setContents(key);
        file.setLength(blob.dataLength);

        updateAttributes(userAttributes, file.getAttributesBuilder());

        if (PARANOID) {
            BlobStore blobStore = getBlobStore(project);
            sanityCheck(blobStore, file);
        }

        storeDirectory(getDirectoryStorage(project), newDir);
    }

    @Override
    @Transactional
    public void deleteFile(Project project, String bucketName, String name) throws CloudException {
        BucketData bucket = getBucket(project, bucketName);

        DirectoryData dir = getDirectoryData(project, bucket);

        DirectoryData.Builder newDir = DirectoryData.newBuilder(dir);

        int foundIndex = findFile(newDir, name);
        if (foundIndex < 0) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        newDir.removeFiles(foundIndex);

        storeDirectory(getDirectoryStorage(project), newDir);
    }

    private static int findFile(DirectoryDataOrBuilder dir, String key) {
        int foundIndex = -1;

        for (int i = 0; i < dir.getFilesCount(); i++) {
            FileData file = dir.getFiles(i);
            if (key.equals(file.getKey())) {
                foundIndex = i;
                break;
            }
        }
        return foundIndex;
    }

    @Override
    public FsFile findFileInfo(FsBucket bucket, String name) throws CloudException {
        DirectoryData dir = getDirectoryData(bucket.getProject(), bucket.getData());

        int foundIndex = findFile(dir, name);
        if (foundIndex == -1) {
            return null;
        }

        FileData found = dir.getFiles(foundIndex);
        return new FsFile(bucket, found);
    }

    @Override
    @Transactional
    public void append(Project project, String bucketName, String name, Long offset, FileBlob blob)
            throws CloudException, IOException {
        getBlobStore(project).put(blob.data);

        BucketData bucket = getBucket(project, bucketName);

        DirectoryData dir = getDirectoryData(project, bucket);

        DirectoryData.Builder newDir = DirectoryData.newBuilder(dir);

        FileData.Builder file = null;

        FileData oldFileData;

        int foundIndex = findFile(newDir, name);
        if (foundIndex != -1) {
            oldFileData = dir.getFiles(foundIndex);
            file = newDir.getFilesBuilder(foundIndex);
        } else {
            // We require the file to already exist because otherwise it's
            // tricky to set metadata etc.
            throw new WebApplicationException(Status.NOT_FOUND);
            // builder = newDir.addFilesBuilder();
            // builder.setKey(name);
        }

        file.setLastModified(System.currentTimeMillis());

        int rangeCount = file.getRangesCount() + 1;
        {
            FileRange.Builder c = file.addRangesBuilder();
            long len = file.getLength();

            if (offset != null) {
                if (len != offset) {
                    throw new IOException("Attempt to append at non-terminal position");
                }
            }

            c.setStart(len);
            c.setEnd(len + blob.dataLength);
            c.setContentKey(blob.hash);
        }

        ResumableMD5Digest md5 = ResumableMD5Digest.get();
        if (file.hasHashResume() && !file.getHashResume().isEmpty()) {
            md5.setState(file.getHashResume(), file.getLength());
            md5.update(blob.data);
        } else {
            BlobStore blobStore = getBlobStore(project);

            CloudObject cloudObject = new CloudObject(oldFileData);
            try (InputStream is = cloudObject.getInputStream(blobStore)) {
                md5.update(is);
            }
            md5.update(blob.data);
        }

        ByteString hashResume = md5.getState();
        ByteString hash = ByteString.copyFrom(md5.digest());

        file.setHash(hash);

        // NOTE: hashResume can contain plaintext. Careful around encryption!
        file.setHashResume(hashResume);

        file.setLength(file.getLength() + blob.dataLength);

        if (PARANOID) {
            BlobStore blobStore = getBlobStore(project);
            sanityCheck(blobStore, file);
        }

        storeDirectory(getDirectoryStorage(project), newDir);

        if (rangeCount >= COMPACTION_THRESHOLD) {
            compactor.enqueue(project, bucketName, name);
        }
    }

    @Override
    @Transactional
    public void deleteBucket(Project project, String bucketName) throws CloudException {
        NamedItemCollection<BucketData> bucketStorage = fileStore.getBuckets(project.getId());

        BucketData bucket = bucketStorage.find(bucketName);
        if (bucket == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        if (bucket.hasRootId()) {
            NumberedItemCollection<DirectoryData> directoryStorage = getDirectoryStorage(project);

            DirectoryData dir = directoryStorage.find(bucket.getRootId());
            if (dir != null) {
                if (!isEmpty(dir)) {
                    throw new WebApplicationException(Status.CONFLICT);
                }
            }
        }

        bucketStorage.delete(bucketName);
    }

    private boolean isEmpty(DirectoryData dir) {
        return dir.getFilesCount() == 0;
    }

    @Override
    @Transactional
    public Status putBucket(Project project, String bucketName, BucketAttributes bucketAttributes,
            Map<String, String> userAttributes) throws CloudException {
        NamedItemCollection<BucketData> bucketStorage = fileStore.getBuckets(project.getId());

        BucketData oldBucket = bucketStorage.find(bucketName);

        BucketData.Builder newBucket;
        boolean isNew;

        if (oldBucket == null) {
            newBucket = BucketData.newBuilder();
            newBucket.setKey(bucketName);

            newBucket.setCreatedAt(Clock.getTimestamp());

            NumberedItemCollection<DirectoryData> directoryStorage = getDirectoryStorage(project);

            fileStore.getDirectories(project.getId());
            DirectoryData.Builder dir = DirectoryData.newBuilder();

            DirectoryData created = storeDirectory(directoryStorage, dir);

            newBucket.setRootId(created.getId());

            isNew = true;
        } else {
            newBucket = BucketData.newBuilder(oldBucket);

            isNew = false;
        }

        updateAttributes(bucketAttributes, newBucket.getBucketAttributesBuilder());
        updateAttributes(userAttributes, newBucket.getAttributesBuilder());

        BucketData updated;

        if (isNew) {
            try {
                updated = bucketStorage.create(newBucket);
            } catch (DuplicateValueException e) {
                // TODO: Is this right? Should we retry?
                throw new WebApplicationException(Status.CONFLICT);
            }
        } else {
            updated = bucketStorage.update(newBucket);
        }

        bucketMetadataUpdated(project, updated);

        return isNew ? Status.CREATED : Status.ACCEPTED;
    }

    private void bucketMetadataUpdated(Project project, BucketData bucket) throws CloudException {
        StorageDerivedMetadata newMetadata = derivedMetadataProvider.get();

        String serviceKey = "bucket/metadata/" + project.getId() + "/" + bucket.getKey();
        newMetadata.apply(project, bucket, serviceKey);
    }

    private void updateAttributes(BucketAttributes src, BucketAttributes.Builder dest) {
        if (src != null) {
            // TODO: Just call merge?
            if (src.hasAclRead()) {
                dest.setAclRead(src.getAclRead());
            }
        }
    }

    private void updateAttributes(Map<String, String> userAttributes, Attributes.Builder builder) {
        if (userAttributes != null) {
            for (Map.Entry<String, String> entry : userAttributes.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                // Key cases are expected to be mangled:
                // https://bugs.launchpad.net/swift/+bug/939982
                key = key.toLowerCase();

                boolean found = false;
                for (KeyValueData.Builder kv : builder.getUserAttributesBuilderList()) {
                    if (key.equals(kv.getKey())) {
                        kv.setValue(value);
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    KeyValueData.Builder kv = builder.addUserAttributesBuilder();
                    kv.setKey(key);
                    kv.setValue(value);
                }
            }
        }
    }

    @Override
    public StreamingOutput open(FsFile file, Long from, Long to) {
        // TODO: This can be made much more efficient

        CloudObject object = new CloudObject(file.getData());

        final InputStream is;
        try {
            BlobStore blobStore = getBlobStore(file.getProject());
            is = object.getInputStream(blobStore, from, to);
        } catch (IOException e) {
            throw new IllegalStateException("Error opening file", e);
        }

        StreamingOutput stream = new StreamingOutput() {
            @Override
            public void write(OutputStream os) throws IOException, WebApplicationException {
                ByteStreams.copy(is, os);
                os.flush();
            }
        };

        return stream;
    }

    @Override
    public FsBucket findBucket(User user, Project project, String bucketName) throws CloudException {
        BucketData bucket = fileStore.getBuckets(project.getId()).find(bucketName);
        if (bucket == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        if (user == null) {
            // Check that the container is public
            boolean allowed = false;

            if (bucket.hasBucketAttributes()) {
                BucketAttributes bucketAttributes = bucket.getBucketAttributes();
                if (bucketAttributes.hasAclRead()) {
                    StorageAcl acl = StorageAcl.parse(AclType.Read, bucketAttributes.getAclRead());

                    // TODO: Do we really care about referer?
                    String referer = "unknown";
                    // String referer = request.getHeader("Referer");

                    if (acl.isRefererAllowed(referer)) {
                        allowed = true;
                    }
                }
            }

            // TODO: Check user access to the container

            if (!allowed) {
                return null;
            }
        }

        return new FsBucket(project, bucket);

    }

    @Override
    @Transactional
    public boolean compact(CompactOperation compaction) throws CloudException, IOException {
        Project project = compaction.getProject();
        String bucketName = compaction.getBucketName();
        String name = compaction.getName();

        BucketData bucket = getBucket(project, bucketName);

        DirectoryData dir = getDirectoryData(project, bucket);

        DirectoryData.Builder newDir = DirectoryData.newBuilder(dir);

        FileData.Builder builder = null;

        FileData oldFileData;

        int foundIndex = findFile(newDir, name);
        if (foundIndex != -1) {
            oldFileData = dir.getFiles(foundIndex);
            builder = newDir.getFilesBuilder(foundIndex);
        } else {
            return false;
        }

        if (builder.getRangesCount() < COMPACTION_THRESHOLD) {
            log.debug("File is (now) smaller than compaction threshold: {}", compaction.getDebugPath());
            return false;
        }

        if (!compaction.compact(this, builder)) {
            // Compactor should have logged a reason
            return false;
        }

        storeDirectory(getDirectoryStorage(project), newDir);
        return true;
    }

    public static void sanityCheck(BlobStore blobStore, FileData.Builder file) {
        Hasher md5 = Hashing.md5().newHasher();
        try (HashingOutputStream hos = new HashingOutputStream(ByteStreams.nullOutputStream(), md5)) {
            List<FileRange> ranges = file.getRangesList();
            for (int i = 0; i < ranges.size(); i++) {
                FileRange range = ranges.get(i);

                BlobData blob = blobStore.find(range.getContentKey());
                if (blob == null) {
                    throw new IllegalStateException("Unable to find blob for range: " + range);
                }

                log.debug("Sanity check: fetch blob {}", Hex.toHex(range.getContentKey().toByteArray()));
                if (blob.size() != (range.getEnd() - range.getStart())) {
                    throw new IllegalStateException();
                }

                blob.copyTo(hos);

                if (i != 0) {
                    FileRange prev = ranges.get(i - 1);

                    if (prev.getEnd() != range.getStart()) {
                        throw new IllegalStateException();
                    }
                } else {
                    if (range.getStart() != 0) {
                        throw new IllegalStateException();
                    }
                }

                if (i == (ranges.size() - 1)) {
                    if (range.getEnd() != file.getLength()) {
                        throw new IllegalStateException();
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Error checking file: " + file, e);
        }

        ByteString hash = ByteString.copyFrom(md5.hash().asBytes());

        if (!file.getHash().equals(hash)) {
            log.warn("Hash mismatch: {} vs {}", Hex.toHex(file.getHash().toByteArray()), Hex.toHex(hash.toByteArray()));
            throw new IllegalStateException("Hash mismatch");
        }
    }

    @Override
    @Transactional
    public ListenableFuture<?> watchBucket(FsBucket bucket, String ifNotEtag) throws CloudException {
        long projectId = bucket.getProject().getId();

        long rootId = bucket.getData().getRootId();

        NumberedItemCollection<DirectoryData> dirs = fileStore.getDirectories(projectId);
        Watched<DirectoryData> watched = dirs.watch(rootId);

        if (ifNotEtag != null) {
            DirectoryData dirData = watched.getValue();
            String currentEtag = computeEtag(dirData);
            if (!ifNotEtag.equals(currentEtag)) {
                return Futures.immediateFuture(currentEtag);
            }
        }

        return watched.getFuture();
    }

    private String computeEtag(DirectoryData dirData) {
        log.warn("Etag computation for directories is stub-implemented");
        return UUID.randomUUID().toString();
    }

    @Override
    public FileInfo getFileInfo(Project project, String bucketName, String name) throws CloudException, IOException {
        BucketData bucketData = getBucket(project, bucketName);
        FsBucket fsBucket = new FsBucket(project, bucketData);

        FsFile fsFile = findFileInfo(fsBucket, name);
        return fsFile;
    }

    List<DirectoryListEntry> listFiles(Project project, FsBucket bucket) throws CloudException {
        NumberedItemCollection<DirectoryData> directoryStorage = getDirectoryStorage(project);

        if (!bucket.data.hasRootId()) {
            return Lists.newArrayList();
        }

        DirectoryData dir = directoryStorage.find(bucket.data.getRootId());
        if (dir == null) {
            throw new IllegalStateException();
        }

        List<DirectoryListEntry> entries = Lists.newArrayList();
        for (FileData file : dir.getFilesList()) {
            entries.add(new DirectoryListEntry(file.getKey(), file));
        }

        return entries;
    }

    @Override
    public List<DirectoryListEntry> listFiles(Project project, FsBucket bucket, String prefix, String delimiter,
            String marker) throws CloudException {
        List<DirectoryListEntry> entries = listFiles(project, bucket);

        if (prefix != null) {
            log.warn("Prefix filter is inefficient");
            List<DirectoryListEntry> matches = Lists.newArrayList();

            for (DirectoryListEntry entry : entries) {
                String name = entry.getKey();
                if (!name.startsWith(prefix)) {
                    continue;
                }

                matches.add(entry);
            }
            entries = matches;
        }

        if (delimiter != null) {
            log.warn("Delimiter filter is inefficient");
            List<DirectoryListEntry> realFiles = Lists.newArrayList();
            Set<String> dirs = Sets.newHashSet();

            boolean changed = false;

            int offset = prefix != null ? prefix.length() : 0;
            for (DirectoryListEntry entry : entries) {
                String name = entry.getKey();
                int nextSlash = name.indexOf(delimiter, offset);
                if (nextSlash == -1) {
                    realFiles.add(entry);
                } else {
                    String key = name.substring(0, nextSlash);
                    dirs.add(key);
                    changed = true;
                }
            }

            if (changed) {
                List<DirectoryListEntry> l = Lists.newArrayList();
                for (String dir : dirs) {
                    l.add(new DirectoryListEntry(dir, null));
                }

                l.addAll(realFiles);
                entries = l;
            }
        }

        if (marker != null) {
            log.warn("Marker filter is inefficient");
            List<DirectoryListEntry> matches = Lists.newArrayList();

            for (DirectoryListEntry entry : entries) {
                String name = entry.getKey();
                if (name.compareTo(marker) <= 0) {
                    continue;
                }

                matches.add(entry);
            }
            entries = matches;
        }
        Collections.sort(entries);
        return entries;
    }

    @Override
    public List<? extends FileInfo> listFiles(Project project, String bucketName, String prefix, String delimiter)
            throws CloudException {
        BucketData bucketData = getBucket(project, bucketName);
        FsBucket fsBucket = new FsBucket(project, bucketData);

        String marker = null;
        return listFiles(project, fsBucket, prefix, delimiter, marker);
    }

    @Override
    public ByteSource getData(Project project, String bucketName, String name, Long from, Long to) throws IOException,
            CloudException {
        BucketData bucketData = getBucket(project, bucketName);
        FsBucket fsBucket = new FsBucket(project, bucketData);

        FsFile fsFile = findFileInfo(fsBucket, name);
        if (fsFile == null) {
            return null;
        }

        CloudObject object = new CloudObject(fsFile.getData());

        BlobStore blobStore = getBlobStore(project);
        return object.asByteSource(blobStore, from, to);
    }

    @Override
    public void ensureBucket(Project project, String bucketName) throws CloudException {
        BucketData bucket = fileStore.getBuckets(project.getId()).find(bucketName);
        if (bucket == null) {
            putBucket(project, bucketName, null, null);
        }
    }

}
