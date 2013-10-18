package io.fathom.cloud.server.resources;

import java.security.PublicKey;

import com.fathomdb.crypto.OpenSshUtils;
import com.google.protobuf.ByteString;

public class ClientCertificate {

    private final ByteString publicKeySha1;

    public ClientCertificate(ByteString publicKeySha1) {
        this.publicKeySha1 = publicKeySha1;
    }

    public ClientCertificate(PublicKey publicKey) {
        this.publicKeySha1 = ByteString.copyFrom(OpenSshUtils.getSignature(publicKey).toByteArray());
    }

    public ByteString getPublicKeySha1() {
        return publicKeySha1;
    }

}
