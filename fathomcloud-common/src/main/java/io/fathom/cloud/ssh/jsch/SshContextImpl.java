package io.fathom.cloud.ssh.jsch;

import io.fathom.cloud.ssh.SftpChannel;
import io.fathom.cloud.ssh.SftpChannelBase;
import io.fathom.cloud.ssh.SftpStat;
import io.fathom.cloud.ssh.SshConfig;
import io.fathom.cloud.ssh.SshContext;
import io.fathom.cloud.ssh.SshDirectTcpipChannel;
import io.fathom.cloud.ssh.SshForwardChannel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathomdb.crypto.bouncycastle.KeyPairs;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.net.InetAddresses;
import com.jcraft.jsch.ChannelDirectTCPIP;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.ChannelSftp.LsEntrySelector;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;

public class SshContextImpl implements SshContext {
    private static final Logger log = LoggerFactory.getLogger(SshContextImpl.class);

    final JSch jsch;

    final String sshUsername;

    final KeyPair sshKeyPair;

    public SshContextImpl(String sshUsername, File privateKeyPath) {
        super();
        this.jsch = new JSch();

        this.sshUsername = sshUsername;
        // this.sshKey = sshKey;

        try {
            this.jsch.addIdentity(privateKeyPath.getAbsolutePath());
        } catch (JSchException e) {
            throw new IllegalArgumentException("Error loading ssh key", e);
        }

        try {
            this.sshKeyPair = KeyPairs.fromPem(privateKeyPath);
        } catch (IOException e) {
            throw new IllegalArgumentException("Error loading ssh key", e);
        }

    }

    class ConnectionState implements SshConfig {
        final InetSocketAddress remote;

        Session session;
        int channelCount;

        @Override
        public String toString() {
            return remote.toString();
        }

        public ConnectionState(InetSocketAddress remote) {
            super();
            this.remote = remote;
        }

        String getInfo() {
            return sshUsername + "@" + remote;
        }

        synchronized Session getSession() throws IOException {
            if (session == null) {
                Session session;
                try {
                    session = jsch.getSession(sshUsername, InetAddresses.toAddrString(remote.getAddress()),
                            remote.getPort());

                    // TODO: Strict host key checking
                    session.setConfig("StrictHostKeyChecking", "no");
                    // session.setPassword("super_secre_password");
                    session.connect();
                } catch (JSchException e) {
                    session = null;
                    throw new IOException("Error connecting to SSH (" + getInfo() + ")", e);
                }

                // ConnectFuture connectFuture;
                // try {
                // connectFuture = sshClient.connect(remote);
                // } catch (Exception e) {
                // throw new IOException(
                // "Error connecting to SSH server: " + remote, e);
                // }
                // long connectTimeoutMillis = 60000;
                // if (!connectFuture.await(connectTimeoutMillis)) {
                // connectFuture.cancel();
                // throw new IOException(
                // "Timeout connecting to SSH server: " + remote);
                // }
                //
                // ClientSession clientSession = connectFuture.getSession();
                //
                // int ret = ClientSession.WAIT_AUTH;
                // while ((ret & ClientSession.WAIT_AUTH) != 0) {
                // int authTimeout = 30000;
                //
                // clientSession.authPublicKey(sshUsername, sshKey);
                // ret = clientSession.waitFor(ClientSession.WAIT_AUTH
                // | ClientSession.CLOSED | ClientSession.AUTHED,
                // authTimeout);
                // }
                //
                // if ((ret & ClientSession.CLOSED) != 0) {
                // throw new IOException(
                // "Unable to authenticate with SSH server: "
                // + remote);
                // }

                this.session = session;
                // this.session = new PooledClientSession(this, session);
            }

            // useCount++;

            return this.session;
        }

        @Override
        public synchronized SftpChannel getSftpChannel() throws IOException {
            Session session = getSession();

            ChannelSftp sftpChannel;
            try {
                sftpChannel = (ChannelSftp) session.openChannel("sftp");

                channelCount++;
                sftpChannel.connect();
            } catch (JSchException e) {
                // TODO: Close session if it's failed??
                channelCount--;
                throw new IOException("Error opening sftp channel (" + getInfo() + ")", e);
            }
            return new JschSftpChannel(sftpChannel);
        }

        public class JschSftpChannel extends SftpChannelBase {
            private final ChannelSftp channel;

            public JschSftpChannel(ChannelSftp channel) {
                this.channel = channel;
            }

            @Override
            public synchronized void close() throws IOException {
                try {
                    channel.exit();
                } finally {
                    channelCount--;
                }
            }

            @Override
            public InputStream open(File file) throws IOException {
                try {
                    return channel.get(file.getPath());
                } catch (SftpException e) {
                    if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                        throw new FileNotFoundException();
                    }
                    log.info("SFTP Error reading file: {} {}", file, e.id);
                    throw new IOException("Error reading file: " + file, e);
                }
            }

            @Override
            public OutputStream writeFile(File file, WriteMode mode) throws IOException {
                try {
                    int jschMode;
                    switch (mode) {
                    case Append:
                        jschMode = ChannelSftp.APPEND;
                        break;

                    case Overwrite:
                        jschMode = ChannelSftp.OVERWRITE;
                        break;

                    default:
                        throw new IllegalArgumentException();
                    }

                    return channel.put(file.getPath(), jschMode);
                } catch (SftpException e) {
                    throw new IOException("Error writing file: " + file, e);
                }
            }

            @Override
            public void delete(File file) throws IOException {
                try {
                    channel.rm(file.getPath());
                } catch (SftpException e) {
                    throw new IOException("Error deleting file: " + file, e);
                }
            }

            @Override
            public SftpStat stat(File file) throws IOException {
                try {
                    SftpATTRS attrs = channel.stat(file.getPath());
                    return new JschSftpStat(attrs);
                } catch (SftpException e) {
                    if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                        return null;
                    }
                    log.info("SFTP Error doing stat on file: {} {}", file, e.id);
                    throw new IOException("Error getting file stat: " + file, e);
                }
            }

            public SftpATTRS lstat(File file) throws IOException {
                SftpATTRS lstat = null;
                try {
                    lstat = channel.lstat(file.getAbsolutePath());
                } catch (SftpException e) {
                    if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                        throw new FileNotFoundException();
                    }
                    throw new IOException("Error during sftp lstat: " + file, e);
                }
                return lstat;
            }

            @Override
            public boolean exists(File file) throws IOException {
                try {
                    channel.lstat(file.getAbsolutePath());
                    return true;
                } catch (SftpException e) {
                    if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                        return false;
                    }
                    throw new IOException("Error during sftp lstat: " + file, e);
                }
            }

            @Override
            public boolean mkdir(File file) throws IOException {
                try {
                    if (exists(file)) {
                        return false;
                    }
                    channel.mkdir(file.getAbsolutePath());
                    return true;
                } catch (SftpException e) {
                    if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                        throw new FileNotFoundException();
                    }
                    throw new IOException("Error during sftp mkdir: " + file, e);
                }
            }

            @Override
            public void mv(File from, File to) throws IOException {
                try {
                    channel.rename(from.getPath(), to.getPath());
                } catch (SftpException e) {
                    throw new IOException("Error during sftp rename: " + from + " to " + to, e);
                }
            }

            @Override
            public List<String> ls(File file) throws IOException {
                try {

                    final List<String> names = Lists.newArrayList();

                    LsEntrySelector selector = new LsEntrySelector() {
                        @Override
                        public int select(LsEntry entry) {
                            // TODO: Filter out directory etc through
                            // attributes...
                            names.add(entry.getFilename());

                            return CONTINUE;

                        }
                    };

                    // TODO: The source code for the ls function is not
                    // confidence-inspiring
                    // TODO: Don't get attributes??
                    channel.ls(file.getPath(), selector);

                    return names;
                } catch (SftpException e) {
                    throw new IOException("Error during sftp ls: " + file, e);
                }
            }

            @Override
            public void chmod(File file, int mode) throws IOException {
                try {
                    channel.chmod(mode, file.getPath());
                } catch (SftpException e) {
                    throw new IOException("Error during sftp chmod: " + file, e);
                }
            }

            @Override
            public void chown(File file, int uid) throws IOException {
                try {
                    channel.chown(uid, file.getPath());
                } catch (SftpException e) {
                    throw new IOException("Error during sftp chown: " + file, e);
                }
            }

            @Override
            public void chgrp(File file, int gid) throws IOException {
                try {
                    channel.chgrp(gid, file.getPath());
                } catch (SftpException e) {
                    throw new IOException("Error during sftp chgrp: " + file, e);
                }
            }
        }

        @Override
        public SshDirectTcpipChannel getDirectTcpipConnection(InetSocketAddress local, InetSocketAddress remote)
                throws IOException {
            Session session = getSession();

            ChannelDirectTCPIP directChannel;
            try {
                directChannel = (ChannelDirectTCPIP) session.openChannel("direct-tcpip");

                directChannel.setHost(InetAddresses.toAddrString(remote.getAddress()));
                directChannel.setPort(remote.getPort());

                directChannel.setOrgIPAddress(InetAddresses.toAddrString(local.getAddress()));
                directChannel.setOrgPort(local.getPort());

                channelCount++;
                directChannel.connect();
            } catch (JSchException e) {
                // TODO: Close session if it's failed??
                channelCount--;
                throw new IOException("Error opening direct-tcpip channel", e);
            }
            return new JschDirectTcpipChannel(directChannel);
        }

        @Override
        public SshForwardChannel forwardLocalPort(InetAddress localAddress, InetSocketAddress remoteSocketAddress)
                throws IOException {
            Session session = getSession();

            String bindAddress = InetAddresses.toAddrString(localAddress);
            int assignedPort;
            ChannelDirectTCPIP directChannel;
            try {
                int port = 0; // Auto asssign
                assignedPort = session.setPortForwardingL(bindAddress, port, remoteSocketAddress.getHostString(),
                        remoteSocketAddress.getPort());

                channelCount++;
            } catch (JSchException e) {
                // TODO: Close session if it's failed??
                channelCount--;
                throw new IOException("Error opening direct-tcpip channel", e);
            }
            return new JschSshForwardChannel(session, bindAddress, assignedPort);
        }

        public class JschSshForwardChannel implements SshForwardChannel {
            private final Session session;
            private final int port;
            private final String bindAddress;

            public JschSshForwardChannel(Session session, String bindAddress, int port) {
                this.session = session;
                this.bindAddress = bindAddress;
                this.port = port;
            }

            @Override
            public synchronized void close() throws IOException {
                try {
                    session.delPortForwardingL(bindAddress, port);
                } catch (JSchException e) {
                    throw new IOException("Error deleting port binding", e);
                } finally {
                    channelCount--;
                }
            }

            @Override
            public InetSocketAddress getLocalSocketAddress() {
                return new InetSocketAddress(bindAddress, port);
            }
        }

        public class JschDirectTcpipChannel implements SshDirectTcpipChannel {
            private final ChannelDirectTCPIP channel;

            public JschDirectTcpipChannel(ChannelDirectTCPIP channel) {
                this.channel = channel;
            }

            @Override
            public synchronized void close() throws IOException {
                try {
                    channel.disconnect();
                } finally {
                    channelCount--;
                }
            }

            @Override
            public InputStream getInputStream() throws IOException {
                return channel.getInputStream();
            }

            @Override
            public OutputStream getOutputStream() throws IOException {
                return channel.getOutputStream();
            }

        }

        @Override
        public int execute(String command, OutputStream stdout, OutputStream stderr) throws IOException {
            Session session = getSession();

            ChannelExec channel;
            try {
                channel = (ChannelExec) session.openChannel("exec");

                channel.setInputStream(null);
                channel.setOutputStream(stdout);
                channel.setErrStream(stderr);

                channel.setCommand(command);

                channelCount++;
                channel.connect();
            } catch (JSchException e) {
                // TODO: Close session if it's failed??
                channelCount--;
                throw new IOException("Error opening direct-tcpip channel", e);
            }

            try {
                while (true) {
                    int exitStatus = channel.getExitStatus();
                    if (exitStatus != -1) {
                        return exitStatus;
                    }

                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Interrupted while waiting for SSH command execution", e);
                    }
                }
            } finally {
                channel.disconnect();
                channelCount--;
            }
        }

        @Override
        public String getUser() {
            return sshUsername;
        }

    }

    final Map<String, ConnectionState> connections = Maps.newHashMap();

    ConnectionState getConnectionState(InetSocketAddress server) {
        String key = InetAddresses.toAddrString(server.getAddress()) + ":" + server.getPort();
        ConnectionState connectionState = connections.get(key);
        if (connectionState == null) {
            connectionState = new ConnectionState(server);
            connections.put(key, connectionState);
        }
        return connectionState;
    }

    @Override
    public InetSocketAddress getRemoteSshAddress(InetSocketAddress address) {
        int sshPort = 22;
        return new InetSocketAddress(address.getAddress(), sshPort);
    }

    @Override
    public SshConfig buildConfig(InetSocketAddress server) {
        ConnectionState connectionState = getConnectionState(server);
        return connectionState;
    }

    @Override
    public PublicKey getPublicKey() {
        return sshKeyPair.getPublic();
        // Vector identities = jsch.getIdentityRepository().getIdentities();
        // Identity identity = (Identity) identities.get(0);
        // byte[] publicKeyBytes = identity.getPublicKeyBlob();
        // String s = new String(publicKeyBytes);
        //
        // return PublicKeys.fromPem(s);
    }

    public KeyPair getKeypair() {
        return sshKeyPair;
    }

}
