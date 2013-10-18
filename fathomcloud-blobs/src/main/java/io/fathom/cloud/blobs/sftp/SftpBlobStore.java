package io.fathom.cloud.blobs.sftp;

import io.fathom.cloud.blobs.BlobData;
import io.fathom.cloud.blobs.BlobStore;
import io.fathom.cloud.blobs.BlobStoreBase;
import io.fathom.cloud.blobs.BlobStoreFactory;
import io.fathom.cloud.blobs.LocalFilesystemBlobStore;
import io.fathom.cloud.blobs.TempFile;
import io.fathom.cloud.sftp.RemoteFile;
import io.fathom.cloud.sftp.Sftp;
import io.fathom.cloud.ssh.SftpChannel;
import io.fathom.cloud.ssh.SshConfig;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathomdb.io.IoUtils;
import com.fathomdb.utils.Hex;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.protobuf.ByteString;

public class SftpBlobStore extends BlobStoreBase {
    private static final Logger log = LoggerFactory.getLogger(SftpBlobStore.class);

    public static class Factory implements BlobStoreFactory {
        final SshConfig sshConfig;
        final RemoteFile remoteBaseDir;
        final File localCacheDir;

        public Factory(SshConfig sshConfig, RemoteFile remoteBaseDir, File localCacheDir) {
            this.sshConfig = sshConfig;
            this.remoteBaseDir = remoteBaseDir;
            this.localCacheDir = localCacheDir;
        }

        @Override
        public BlobStore get(String key) throws IOException {
            Preconditions.checkArgument(!Strings.isNullOrEmpty(key));
            Preconditions.checkArgument(LocalFilesystemBlobStore.isSafeFileName(key));

            return new SftpBlobStore(sshConfig, new RemoteFile(remoteBaseDir, key), new File(localCacheDir, key));
        }
    }

    final RemoteFile remoteBaseDir;
    final RemoteFile tmpDir;

    final SshConfig sshConfig;

    private final File localCacheDir;

    private final File localCacheDirTmp;

    // TODO: Use pool? Move to SshChannel??
    // private final SimplePool<SftpChannel> POOL = new
    // SimplePool<SftpChannel>();
    //
    // private SftpChannel borrowSftp() throws CloudException {
    // SftpChannel sftp = POOL.poll();
    // if (sftp == null) {
    // sftp = buildSftp();
    // }
    // return sftp;
    // }

    public SftpBlobStore(SshConfig sshConfig, RemoteFile remoteBaseDir, File localCacheDir) throws IOException {
        super();
        this.sshConfig = sshConfig;
        this.remoteBaseDir = remoteBaseDir;
        this.localCacheDir = localCacheDir;

        this.tmpDir = new RemoteFile(remoteBaseDir, "_tmp");
        mkdirs(tmpDir);

        localCacheDirTmp = new File(localCacheDir, "_tmp");
        IoUtils.mkdirs(localCacheDirTmp);
    }

    void mkdirs(RemoteFile dir) throws IOException {
        try (Sftp sftp = buildSftp()) {
            sftp.mkdirs(dir.getSshPath());
        }
    }

    protected Sftp buildSftp() throws IOException {
        SftpChannel sftp = sshConfig.getSftpChannel();
        return new Sftp(sftp, tmpDir);
    }

    @Override
    public BlobData find(ByteString key) throws IOException {
        RemoteFile remoteFile = buildRemoteFile(key);

        File localCache;
        try {
            localCache = buildCacheFile(key, true);
        } catch (IOException e) {
            throw new IOException("Error creating local cache file", e);
        }

        if (localCache.exists()) {
            // TODO: Verify?
            log.debug("Using local cache file: {}", localCache);
        } else {
            try (TempFile localTemp = TempFile.in(this.localCacheDirTmp)) {
                HashCode md5;

                try (Sftp sftp = buildSftp(); InputStream is = sftp.open(remoteFile.getSshPath())) {
                    try (OutputStream os = new FileOutputStream(localTemp.getFile())) {
                        Hasher hasher = Hashing.md5().newHasher();
                        byte[] buffer = new byte[8192];
                        while (true) {
                            int n = is.read(buffer);
                            if (n == -1) {
                                break;
                            }

                            hasher.putBytes(buffer, 0, n);
                            os.write(buffer, 0, n);
                        }
                        md5 = hasher.hash();
                    }
                } catch (FileNotFoundException e) {
                    // Unable to find sftp file
                    return null;
                }

                if (!matches(key, md5)) {
                    throw new IOException("Copied file, but did not match hash");
                }

                if (!localTemp.renameTo(localCache)) {
                    // TODO: Maybe someone else concurrently downloaded it??
                    throw new IOException("Could not rename file to cache file");
                }
            }
        }

        if (localCache.exists()) {
            BlobData is = new BlobData(localCache, key);
            return is;
        } else {
            return null;
        }
    }

    @Override
    public boolean has(ByteString key, boolean checkCache) throws IOException {
        if (checkCache) {
            File localCache = buildCacheFile(key);

            if (localCache.exists()) {
                return true;
            }
        }

        RemoteFile remoteFile = buildRemoteFile(key);

        try (Sftp sftp = buildSftp()) {
            return sftp.exists(remoteFile.getSshPath());
        }
    }

    private static boolean matches(ByteString a, HashCode b) {
        return Arrays.equals(a.toByteArray(), b.asBytes());
    }

    private RemoteFile buildRemoteFile(Sftp sftp, ByteString key, boolean mkdirs) throws IOException {
        RemoteFile file = buildRemoteFile(key);

        if (mkdirs) {
            // TODO: Cache created / not created state?
            sftp.mkdirs(file.getParentFile().getSshPath());
        }

        return file;
    }

    private RemoteFile buildRemoteFile(ByteString key) {
        File file = buildFile(this.remoteBaseDir.getSshPath(), key);
        RemoteFile remoteFile = new RemoteFile(file);
        return remoteFile;
    }

    private File buildCacheFile(ByteString key, boolean mkdirs) throws IOException {
        File file = buildCacheFile(key);

        if (mkdirs) {
            // TODO: Cache created / not created state?
            IoUtils.mkdirs(file.getParentFile());
        }

        return file;
    }

    private File buildCacheFile(ByteString key) {
        File file = buildFile(this.localCacheDir, key);

        return file;
    }

    @Override
    public void put(BlobData data) throws IOException {
        ByteString key = data.getHash();

        File localCacheFile;
        try {
            localCacheFile = buildCacheFile(key, true);
        } catch (IOException e) {
            throw new IOException("Error creating local cache file", e);
        }

        // TODO: Can we be more efficient if BlobData is itself a temp file?

        try (TempFile localTemp = TempFile.in(localCacheDirTmp)) {
            localTemp.copyFrom(data);

            if (!localTemp.renameTo(localCacheFile)) {
                // TODO: Maybe someone else concurrently downloaded it??
                throw new IOException("Could not rename file to cache file");
            }
        }

        try (Sftp sftp = buildSftp()) {
            RemoteFile remoteFile = buildRemoteFile(sftp, key, true);

            if (sftp.exists(remoteFile.getSshPath())) {
                // TODO: Verify that the file contents match??
                log.warn("TODO: Validate that remote files match for sftp");
                return;
            }

            sftp.writeAtomic(remoteFile, localCacheFile);
        }
    }

    @Override
    public Iterable<ByteString> listWithPrefix(String prefix) throws IOException {
        List<ByteString> ret = Lists.newArrayList();

        try (Sftp sftp = buildSftp()) {
            for (String name : sftp.ls(remoteBaseDir.getSshPath())) {
                if (name.charAt(0) == '.') {
                    continue;
                }

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

                listWithPrefix(sftp, ret, new RemoteFile(remoteBaseDir, name), prefix);
            }
        }

        return ret;
    }

    private void listWithPrefix(SftpChannel sftp, List<ByteString> ret, RemoteFile dir, String prefix)
            throws IOException {
        for (String name : sftp.ls(dir.getSshPath())) {
            if (name.charAt(0) == '.') {
                continue;
            }

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

    @Override
    public String toString() {
        return "SftpBlobStore [" + sshConfig + ":" + remoteBaseDir + "]";
    }

}
