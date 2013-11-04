package io.fathom.cloud.commands;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.PublicKey;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.apache.sshd.SshServer;
import org.apache.sshd.agent.SshAgent;
import org.apache.sshd.agent.SshAgentFactory;
import org.apache.sshd.agent.SshAgentServer;
import org.apache.sshd.common.Channel;
import org.apache.sshd.common.Factory;
import org.apache.sshd.common.ForwardingAcceptorFactory;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.Session;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.FileSystemFactory;
import org.apache.sshd.server.FileSystemView;
import org.apache.sshd.server.ForwardingFilter;
import org.apache.sshd.server.PublickeyAuthenticator;
import org.apache.sshd.server.UserAuth;
import org.apache.sshd.server.auth.UserAuthPublicKey;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathomdb.Configuration;
import com.fathomdb.crypto.OpenSshUtils;
import com.google.common.base.CharMatcher;
import com.google.common.base.Charsets;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

@Singleton
public class EmbeddedSshd {
    private static final Logger log = LoggerFactory.getLogger(EmbeddedSshd.class);

    public static final String CONFIG_ENABLED = "sshd.enabled";

    @Inject
    Configuration configuration;

    private SshServer sshd;

    @Inject
    SshCommandFactory sshCommandFactory;

    public boolean start() throws IOException {
        if (sshd != null) {
            throw new IllegalStateException();
        }

        final boolean authUseKeys = true;

        // TODO: Use AuthorizedSshKeys helper
        final File authorizedKeys = configuration.lookupFile("sshd.authorized_keys", "~/.ssh/authorized_fathomcloud");

        if (!authUseKeys) {
            throw new IllegalArgumentException(
                    "Neither password nor publickey SSH-auth are enabled, so won't start SSH server");
        }

        sshd = SshServer.setUpDefaultServer();

        if (authUseKeys) {
            sshd.setPublickeyAuthenticator(new PublickeyAuthenticator() {
                @Override
                public boolean authenticate(String username, PublicKey key, ServerSession session) {
                    try {
                        String opensshEncoded = OpenSshUtils.serialize(key);
                        log.info("SSH presented public key: " + opensshEncoded);

                        List<String> match = Lists.newArrayList(Splitter.on(CharMatcher.WHITESPACE).split(
                                opensshEncoded));

                        if (!authorizedKeys.exists()) {
                            log.warn("SSH authorized key file does not exist: {}", authorizedKeys);
                        } else {
                            for (String line : Files.readLines(authorizedKeys, Charsets.UTF_8)) {
                                line = line.trim();
                                List<String> tokens = Lists.newArrayList(Splitter.on(CharMatcher.WHITESPACE)
                                        .split(line));
                                if (tokens.size() < 2) {
                                    continue;
                                }

                                if (!match.get(0).equals(tokens.get(0))) {
                                    continue;
                                }
                                if (!match.get(1).equals(tokens.get(1))) {
                                    continue;
                                }

                                return true;
                            }
                        }

                        return false;
                    } catch (Exception e) {
                        log.error("Exception while verifying public key", e);
                        return false;
                    }
                }
            });
        }

        // Log if anyone even suggests touching the filesystem
        sshd.setFileSystemFactory(new FileSystemFactory() {
            @Override
            public FileSystemView createFileSystemView(Session session) throws IOException {
                log.warn("Blocking SSHD filesystem access");
                throw new IllegalStateException("Blocking SSHD filesystem access");
            }
        });

        // Log if anyone even suggested forwarding a port
        sshd.setForwardingFilter(new ForwardingFilter() {
            @Override
            public boolean canListen(InetSocketAddress address, ServerSession session) {
                log.warn("Blocking SSHD port forwarding");
                return false;
            }

            @Override
            public boolean canForwardX11(ServerSession session) {
                log.warn("Blocking SSHD port forwarding");
                return false;
            }

            @Override
            public boolean canForwardAgent(ServerSession session) {
                log.warn("Blocking SSHD port forwarding");
                return false;
            }

            @Override
            public boolean canConnect(InetSocketAddress address, ServerSession session) {
                log.warn("Blocking SSHD port forwarding");
                return false;
            }
        });

        sshd.setAgentFactory(new SshAgentFactory() {
            @Override
            public NamedFactory<Channel> getChannelForwardingFactory() {
                log.warn("Blocking SSHD agent functionality");
                throw new IllegalStateException("Blocking SSHD agent functionality");
            }

            @Override
            public SshAgentServer createServer(Session session) throws IOException {
                log.warn("Blocking SSHD agent functionality");
                throw new IllegalStateException("Blocking SSHD agent functionality");
            }

            @Override
            public SshAgent createClient(Session session) throws IOException {
                log.warn("Blocking SSHD agent functionality");
                throw new IllegalStateException("Blocking SSHD agent functionality");
            }
        });

        sshd.setTcpipForwardNioSocketAcceptorFactory(new ForwardingAcceptorFactory() {
            @Override
            public NioSocketAcceptor createNioSocketAcceptor(Session session) {
                log.warn("Blocking SSHD Tcpip Forward functionality");
                throw new IllegalStateException("Blocking SSHD Tcpip Forward access");
            }
        });

        sshd.setX11ForwardNioSocketAcceptorFactory(new ForwardingAcceptorFactory() {
            @Override
            public NioSocketAcceptor createNioSocketAcceptor(Session session) {
                log.warn("Blocking SSHD X11 Forward functionality");
                throw new IllegalStateException("Blocking SSHD X11 Forward access");
            }
        });

        sshd.setShellFactory(new Factory<Command>() {
            @Override
            public Command create() {
                log.warn("Blocking SSHD shell functionality");
                throw new IllegalStateException("Blocking SSHD shell access");
            }
        });

        sshd.setPort(configuration.lookup("sshd.port", 2222));
        String hostkeyPath = configuration.lookup("sshd.hostkey", "hostkey.ser");
        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(hostkeyPath));
        sshd.setCommandFactory(sshCommandFactory);

        List<NamedFactory<Command>> subsystemFactories = sshd.getSubsystemFactories();
        if (subsystemFactories != null) {
            throw new UnsupportedOperationException();
        }

        List<NamedFactory<Channel>> channelFactories = sshd.getChannelFactories();
        sshd.setChannelFactories(Lists.newArrayList(Iterables.filter(channelFactories,
                new Predicate<NamedFactory<Channel>>() {
                    @Override
                    public boolean apply(NamedFactory<Channel> command) {
                        if (command instanceof ChannelSession.Factory) {
                            log.info("Accepting Channel factory: " + command);
                            return true;
                        } else {
                            return false;
                        }
                    }
                })));

        log.info("Starting SSH server");
        sshd.start();

        for (NamedFactory<UserAuth> authFactory : sshd.getUserAuthFactories()) {
            if (authFactory instanceof UserAuthPublicKey.Factory) {
                if (!authUseKeys) {
                    throw new IllegalStateException();
                }
            } else {
                throw new IllegalStateException();
            }
        }

        return true;
    }

    public void stop() throws InterruptedException {
        if (sshd != null) {
            sshd.stop();
            sshd = null;
        }
    }
}
