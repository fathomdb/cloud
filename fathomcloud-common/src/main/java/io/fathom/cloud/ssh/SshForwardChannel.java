package io.fathom.cloud.ssh;

import java.net.InetSocketAddress;

public interface SshForwardChannel extends SshChannel {

    InetSocketAddress getLocalSocketAddress();

}
