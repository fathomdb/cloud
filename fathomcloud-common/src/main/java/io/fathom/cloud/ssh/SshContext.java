package io.fathom.cloud.ssh;

import java.net.InetSocketAddress;
import java.security.PublicKey;

public interface SshContext {
    PublicKey getPublicKey();

    SshConfig buildConfig(InetSocketAddress server);

    InetSocketAddress getRemoteSshAddress(InetSocketAddress address);
}
