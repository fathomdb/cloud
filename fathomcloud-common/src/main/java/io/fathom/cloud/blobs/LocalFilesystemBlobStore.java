package io.fathom.cloud.blobs;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathomdb.io.IoUtils;
import com.fathomdb.utils.Hex;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import com.google.protobuf.ByteString;

public class LocalFilesystemBlobStore extends BlobStoreBase {
    private static final Logger log = LoggerFactory.getLogger(LocalFilesystemBlobStore.class);

    public static class Factory implements BlobStoreFactory {
        final String baseDir;

        public Factory(String baseDir) {
            this.baseDir = baseDir;
        }

        @Override
        public BlobStore get(String key) throws IOException {
            Preconditions.checkArgument(!Strings.isNullOrEmpty(key));
            Preconditions.checkArgument(isSafeFileName(key));

            File dir = new File(baseDir, key);
            return new LocalFilesystemBlobStore(dir);
        }
    }

    final File basedir;
    final File tmpdir;
    final File queueDir;

    public LocalFilesystemBlobStore(File basedir) throws IOException {
        super();
        this.basedir = basedir;

        this.tmpdir = new File(basedir, "_tmp");
        IoUtils.mkdirs(this.tmpdir);

        this.queueDir = new File(basedir, "_queue");
    }

    public static boolean isSafeFileName(String key) {
        int len = key.length();
        for (int i = 0; i < len; i++) {
            char c = key.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                continue;
            }

            switch (c) {
            case '_':
            case '-':
                break;

            default:
                return false;
            }
        }
        return true;
    }

    @Override
    public BlobData find(ByteString key) throws IOException {
        File file = buildFile(key);

        if (file.exists()) {
            return new BlobData(file, key);
        } else {
            return null;
        }
    }

    @Override
    public boolean has(ByteString key, boolean checkCache) {
        File file = buildFile(key);
        return file.exists();
    }

    private File buildFile(ByteString key, boolean mkdirs) throws IOException {
        File file = buildFile(key);

        if (mkdirs) {
            // TODO: Cache created / not created state?
            IoUtils.mkdirs(file.getParentFile());
        }

        return file;
    }

    private File buildFile(ByteString key) {
        File file = buildFile(basedir, key);
        return file;
    }

    @Override
    public void put(BlobData data) throws IOException {
        ByteString key = data.getHash();

        File file = buildFile(key, true);

        if (file.exists()) {
            // TODO: Verify that the file contents match??
            if (isSame(file, data)) {
                return;
            } else {
                throw new UnsupportedOperationException();
            }
        }

        try (TempFile tempFile = TempFile.in(tmpdir)) {
            tempFile.copyFrom(data);
            tempFile.renameTo(file);
        }
    }

    private boolean isSame(File a, BlobData b) throws IOException {
        ByteSource supplierA = Files.asByteSource(a);
        // ByteSource supplierB = ByteStreams.asByteSource(b);

        return supplierA.contentEquals(b);
    }

    @Override
    public Iterable<ByteString> listWithPrefix(String prefix) throws IOException {
        List<ByteString> ret = Lists.newArrayList();

        for (File dir : basedir.listFiles()) {
            if (!dir.isDirectory()) {
                continue;
            }

            String name = dir.getName();
            if (name.charAt(0) == '_') {
                continue;
            }

            if (prefix.length() > name.length()) {
                if (!prefix.startsWith(name)) {
                    continue;
                }
            } else {
                if (!name.startsWith(prefix)) {
                    continue;
                }
            }

            listWithPrefix(ret, dir, prefix);
        }

        return ret;
    }

    private void listWithPrefix(List<ByteString> ret, File dir, String prefix) {
        for (File file : dir.listFiles()) {
            if (!file.isFile()) {
                continue;
            }

            String name = file.getName();
            if (!name.startsWith(prefix)) {
                continue;
            }

            try {
                byte[] bytes = Hex.fromHex(name);
                ret.add(ByteString.copyFrom(bytes));
            } catch (IllegalArgumentException e) {
                log.warn("Unable to parse filename: " + name, e);
                continue;
            }
        }
    }

}
