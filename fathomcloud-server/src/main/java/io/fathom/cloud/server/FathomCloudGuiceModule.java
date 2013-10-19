package io.fathom.cloud.server;

import io.fathom.cloud.blobs.BlobStoreFactory;
import io.fathom.cloud.blobs.replicated.ReplicatedBlobStore;
import io.fathom.cloud.keyczar.KeyczarFactory;
import io.fathom.cloud.keyczar.ZookeeperKeyczarFactory;
import io.fathom.cloud.mq.MessageQueueService;
import io.fathom.cloud.mq.MessageQueueServiceImpl;
import io.fathom.cloud.server.auth.AuthProvider;
import io.fathom.cloud.server.auth.SharedKeystore;
import io.fathom.cloud.server.auth.SharedSecretTokenService;
import io.fathom.cloud.server.auth.TokenService;
import io.fathom.cloud.server.auth.WebAuthProvider;
import io.fathom.cloud.ssh.SshContext;
import io.fathom.cloud.ssh.jsch.SshContextImpl;
import io.fathom.cloud.state.StateStore;
import io.fathom.cloud.state.ZookeeperStateStore;
import io.fathom.cloud.state.zookeeper.KeyczarSharedKeystore;
import io.fathom.cloud.zookeeper.ExternalZookeeper;
import io.fathom.cloud.zookeeper.ZookeeperClient;
import io.fathom.cloud.zookeeper.ZookeeperCluster;
import io.fathom.cloud.zookeeper.ZookeeperClusterClientProvider;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.security.KeyPair;

import org.bouncycastle.openssl.PEMReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathomdb.Configuration;
import com.fathomdb.crypto.DirectoryEncryptionStore;
import com.fathomdb.crypto.EncryptionStore;
import com.fathomdb.crypto.bouncycastle.BouncyCastleLoader;
import com.fathomdb.io.IoUtils;
import com.fathomdb.server.http.JettyWebServerBuilder;
import com.fathomdb.server.http.WebServerBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.name.Names;

public class FathomCloudGuiceModule extends AbstractModule {
    private static final Logger log = LoggerFactory.getLogger(FathomCloudGuiceModule.class);

    final Configuration configuration;

    public FathomCloudGuiceModule(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    protected void configure() {
        EncryptionStore encryptionStore = new DirectoryEncryptionStore(configuration.lookupFile("ssl.path",
                "/var/fathomcloud/keystore/"));
        bind(EncryptionStore.class).toInstance(encryptionStore);

        bind(MessageQueueService.class).to(MessageQueueServiceImpl.class);

        bind(AuthProvider.class).to(WebAuthProvider.class);

        bind(WebServerBuilder.class).to(JettyWebServerBuilder.class);

        bind(Configuration.class).to(ClusterConfiguration.class);

        bind(Configuration.class).annotatedWith(Names.named("instance")).toInstance(configuration);

        // InMemoryStateStore stateStore = new InMemoryStateStore();
        // bind(StateStore.class).toInstance(stateStore);

        if (configuration.find("zookeeper.embedded.basedir") != null) {
            throw new UnsupportedOperationException();
            // bind(ZookeeperCluster.class).to(EmbeddedZookeeper.class).asEagerSingleton();
        } else {
            bind(ZookeeperCluster.class).to(ExternalZookeeper.class).asEagerSingleton();
        }
        bind(ZookeeperClient.class).toProvider(ZookeeperClusterClientProvider.class).in(Scopes.SINGLETON);

        bind(StateStore.class).to(ZookeeperStateStore.class);

        SshContext sshContext;
        {
            String currentUser = System.getProperty("user.name");
            String sshUsername = configuration.lookup("ssh.user", currentUser);

            File keyFile = configuration.lookupFile("ssh.key", "~/.ssh/id_rsa");

            sshContext = new SshContextImpl(sshUsername, keyFile);
        }
        bind(SshContext.class).toInstance(sshContext);

        bind(BlobStoreFactory.class).to(ReplicatedBlobStore.Factory.class).in(Scopes.SINGLETON);

        bind(KeyczarFactory.class).to(ZookeeperKeyczarFactory.class).asEagerSingleton();

        bind(SharedKeystore.class).to(KeyczarSharedKeystore.class).asEagerSingleton();

        bind(TokenService.class).to(SharedSecretTokenService.class).asEagerSingleton();
    }

    private KeyPair deserializeSshKey(String keyData) throws IOException {
        PEMReader r = new PEMReader(new StringReader(keyData), null, BouncyCastleLoader.getName());
        try {
            return (KeyPair) r.readObject();
        } finally {
            IoUtils.safeClose(r);
        }
    }

}
