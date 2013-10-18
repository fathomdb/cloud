package io.fathom.cloud.storage;

import io.fathom.cloud.blobs.BlobData;
import io.fathom.cloud.blobs.BlobStore;
import io.fathom.cloud.blobs.TempFile;
import io.fathom.cloud.io.HashingOutputStream;
import io.fathom.cloud.protobuf.FileModel.FileRange;
import io.fathom.cloud.protobuf.FileModel.FileData.Builder;
import io.fathom.cloud.server.model.Project;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.protobuf.ByteString;

/**
 * We create a CompactOperation so that we can (in future) remember that we've
 * combined blocks
 * 
 */
public class CompactOperation {

    private static final Logger log = LoggerFactory.getLogger(CompactOperation.class);

    private static final boolean PARANOID = true;

    final Project project;
    final String bucketName;
    final String name;

    public CompactOperation(Project project, String bucketName, String name) {
        super();
        this.project = project;
        this.bucketName = bucketName;
        this.name = name;
    }

    public Project getProject() {
        return project;
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getName() {
        return name;
    }

    public boolean compact(FileServiceInternal fs, Builder file) throws IOException {
        int n = file.getRangesCount();

        // Quick sanity check
        if (n < 2) {
            return false;
        }

        int bestStart = 0;
        int bestEnd = -1;
        float bestScore = Float.MAX_VALUE;

        // Try to bring it down to a small number of segments; we don't want to
        // immediately re-compact
        int minLength = Math.max(n - FileServiceImpl.COMPACTION_THRESHOLD / 2, 2);
        log.debug("Chose minimum merge length of {}", minLength);

        for (int start = 0; start < n; start++) {
            long maxSize = 0;
            long minSize = Long.MAX_VALUE;
            long totalSize = 0;

            for (int end = start; end < n; end++) {
                FileRange range = file.getRanges(end);

                long size = range.getEnd() - range.getStart();
                maxSize = Math.max(maxSize, size);
                minSize = Math.min(minSize, size);
                totalSize += size;
                int len = end - start;

                if (len >= minLength) {
                    // This is loosely/directly based on Lucene's tiered merge
                    // policy

                    // final float skew = ((float) maxSize) / ((float) minSize);
                    final float skew = ((float) maxSize) / ((float) totalSize);

                    // Strongly favor merges with less skew (smaller
                    // mergeScore is better):
                    float mergeScore = skew;

                    // Gently favor smaller merges over bigger ones. We
                    // don't want to make this exponent too large else we
                    // can end up doing poor merges of small segments in
                    // order to avoid the large merges:
                    mergeScore *= Math.pow(totalSize, 0.05);

                    if (mergeScore < bestScore) {
                        bestScore = mergeScore;
                        bestStart = start;
                        bestEnd = end;
                    }

                    // log.debug("{} - {} => {}", new Object[] { start, end,
                    // mergeScore });
                }

            }
        }

        if (bestEnd == -1) {
            log.warn("Unable to find any merges!");
            return false;
        }

        for (int i = 0; i < n; i++) {
            FileRange range = file.getRanges(i);
            long len = range.getEnd() - range.getStart();
            log.info("{} {}", i, len);
        }
        log.info("Chose merge {}-{}", bestStart, bestEnd);

        BlobStore blobStore = fs.getBlobStore(project);
        List<FileRange> newRanges = Lists.newArrayList();

        for (int i = 0; i < bestStart; i++) {
            newRanges.add(file.getRanges(i));
        }

        try (TempFile tempFile = TempFile.create()) {
            FileRange.Builder c = FileRange.newBuilder();

            Hasher md5 = Hashing.md5().newHasher();
            try (OutputStream fos = new HashingOutputStream(new FileOutputStream(tempFile.getFile()), md5)) {
                for (int i = bestStart; i < bestEnd; i++) {
                    FileRange range = file.getRanges(i);
                    if (i == bestStart) {
                        c.setStart(range.getStart());
                    }
                    if (i == (bestEnd - 1)) {
                        c.setEnd(range.getEnd());
                    }

                    final BlobData blob = blobStore.find(range.getContentKey());
                    if (blob == null) {
                        throw new IOException("Unable to open storage for range: " + range);
                    }

                    blob.copyTo(fos);
                }
            }

            if (!c.hasStart() || !c.hasEnd()) {
                throw new IllegalStateException();
            }

            ByteString hash = ByteString.copyFrom(md5.hash().asBytes());
            BlobData blobData = new BlobData(tempFile.getFile(), hash);
            blobStore.put(blobData);

            if (PARANOID) {
                BlobData blob = blobStore.find(blobData.getHash());
                if (blob == null) {
                    throw new IllegalStateException();
                }
                ByteString checkHash = ByteString.copyFrom(blob.hash(Hashing.md5()).asBytes());
                if (!blobData.getHash().equals(checkHash)) {
                    log.warn("Hash mismatch: {} vs {}", blobData.getHash(), checkHash);
                    throw new IllegalStateException();
                }
            }
            c.setContentKey(blobData.getHash());

            newRanges.add(c.build());
        }

        for (int i = bestEnd; i < file.getRangesCount(); i++) {
            newRanges.add(file.getRanges(i));
        }

        file.clearRanges();
        file.addAllRanges(newRanges);

        if (PARANOID) {
            FileServiceImpl.sanityCheck(blobStore, file);
            log.debug("File before modification OK");
        }

        if (PARANOID) {
            FileServiceImpl.sanityCheck(blobStore, file);
            log.debug("File after modification OK");
        }

        // log.info("New file: {}", file.clone().build());

        return true;
    }

    public String getDebugPath() {
        String path = project.getId() + "/" + bucketName + "/" + name;
        return path;
    }

    @Override
    public String toString() {
        return "CompactOperation [path=" + getDebugPath() + "]";
    }

}
