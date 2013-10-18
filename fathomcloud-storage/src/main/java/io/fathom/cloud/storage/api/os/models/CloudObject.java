package io.fathom.cloud.storage.api.os.models;

import io.fathom.cloud.blobs.BlobData;
import io.fathom.cloud.blobs.BlobStore;
import io.fathom.cloud.protobuf.FileModel.FileData;
import io.fathom.cloud.protobuf.FileModel.FileRange;

import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import com.fathomdb.utils.Hex;
import com.google.common.collect.Lists;
import com.google.common.io.ByteSource;
import com.google.common.io.ByteStreams;
import com.google.protobuf.ByteString;

public class CloudObject {
    protected static final ByteSource EMPTY = ByteStreams.asByteSource(new byte[0]);
    final FileData data;

    public CloudObject(FileData data) {
        this.data = data;
    }

    public InputStream getInputStream(final BlobStore blobStore) throws IOException {
        return getInputStream(blobStore, null, null);
    }

    public InputStream getInputStream(final BlobStore blobStore, final Long from, final Long to) throws IOException {
        // if (data.getRangesCount() == 1) {
        // return getInputStream(keyValueStore, data.getRanges(0)
        // .getContentKey());
        // } else {
        // TODO: This can be made much more efficient

        List<FileRange> ranges = data.getRangesList();
        if (from != null || to != null) {
            List<FileRange> matching = Lists.newArrayList();
            for (FileRange range : ranges) {
                if (from != null) {
                    if (from > range.getEnd()) {
                        continue;
                    }
                }
                if (to != null) {
                    if (to <= range.getStart()) {
                        continue;
                    }
                }

                matching.add(range);
            }
            ranges = matching;
        }

        final Iterator<FileRange> it = data.getRangesList().iterator();

        SequenceInputStream sis = new SequenceInputStream(new Enumeration<InputStream>() {

            @Override
            public boolean hasMoreElements() {
                return it.hasNext();
            }

            @Override
            public InputStream nextElement() {
                ByteSource is;
                try {
                    FileRange range = it.next();
                    is = getBlob(blobStore, range.getContentKey());

                    if (to != null || from != null) {
                        long start = 0;
                        if (from != null) {
                            start = Math.max(0, from - range.getStart());

                            // Enforced by the skipping logic
                            assert start <= is.size();
                        }
                        long end = is.size();
                        if (to != null) {
                            end = Math.min(to - range.getStart(), is.size());

                            // Enforced by the skipping logic
                            assert end >= 0;
                        }

                        long length = end - start;
                        if (length <= 0) {
                            assert length == 0;
                            // Easier than worrying about hasNext
                            is = EMPTY;
                        } else {
                            is = is.slice(start, length);
                        }
                    }

                    // TODO: Open buffered stream??

                    return is.openStream();
                } catch (IOException e) {
                    throw new IllegalStateException("Error reading data", e);
                }
            }
        });

        return sis;
        // }
    }

    BlobData getBlob(BlobStore blobStore, ByteString key) throws IOException {
        final BlobData is = blobStore.find(key);

        if (is == null) {
            throw new IOException("Unable to open storage for range: " + Hex.toHex(key.toByteArray()));
        }

        return is;
    }

    public ByteSource asByteSource(final BlobStore blobStore, final Long from, final Long to) {
        return new ByteSource() {
            @Override
            public InputStream openStream() throws IOException {
                return getInputStream(blobStore, from, to);
            }
        };
    }

}
