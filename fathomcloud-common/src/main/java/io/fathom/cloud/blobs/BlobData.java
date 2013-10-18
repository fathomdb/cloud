package io.fathom.cloud.blobs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import com.google.protobuf.ByteString;

public class BlobData extends ByteSource
// implements InputSupplier<InputStream> {
{
    final ByteSource data;
    final long size;
    final ByteString hash;

    @Override
    public long size() {
        return size;
    }

    public BlobData(ByteSource data, ByteString hash) throws IOException {
        super();
        this.data = data;
        this.hash = hash;
        this.size = data.size();
    }

    public BlobData(File file, ByteString hash) throws IOException {
        this(Files.asByteSource(file), hash);
    }

    // @Override
    // public InputStream getInput() throws IOException {
    // return openStream();
    // }

    @Override
    public InputStream openStream() throws IOException {
        return data.openStream();
    }

    public Object asEntity() {
        return data;
    }

    public ByteString getHash() {
        return hash;
    }

    public void close() {

    }

    public static BlobData build(ByteSource src) throws IOException {
        HashCode hashCode = src.hash(Hashing.md5());
        ByteString hash = ByteString.copyFrom(hashCode.asBytes());

        return new BlobData(src, hash);
    }

    public static BlobData build(File file) throws IOException {
        return build(Files.asByteSource(file));
    }
}
