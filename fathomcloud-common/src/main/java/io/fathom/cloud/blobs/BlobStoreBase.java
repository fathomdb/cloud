package io.fathom.cloud.blobs;

import java.io.File;

import com.fathomdb.utils.Hex;
import com.google.protobuf.ByteString;

public abstract class BlobStoreBase implements BlobStore {

    protected static File buildFile(File base, ByteString key) {
        byte[] bytes = key.toByteArray();
        String hex = Hex.toHex(bytes);
        String prefix = hex.substring(0, 3);
        File parent = new File(base, prefix);

        return new File(parent, hex);
    }

}
