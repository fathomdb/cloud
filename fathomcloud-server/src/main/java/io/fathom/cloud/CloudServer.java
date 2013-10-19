package io.fathom.cloud;

import io.fathom.cloud.cluster.ServiceRegistration;
import io.fathom.cloud.commands.EmbeddedSshd;
import io.fathom.cloud.identity.secrets.Secrets;
import io.fathom.cloud.keyczar.KeyczarFactory;
import io.fathom.cloud.lifecycle.Lifecycle;
import io.fathom.cloud.log.LogbackHook;
import io.fathom.cloud.persist.ZookeeperPersistModule;
import io.fathom.cloud.server.FathomCloudGuiceModule;
import io.fathom.cloud.server.OpenstackServerServletModule;
import io.fathom.cloud.server.auth.SharedSecretTokenService;
import io.fathom.cloud.zookeeper.ZookeeperClient;

import java.io.IOException;
import java.net.InetAddress;
import java.util.EnumSet;
import java.util.List;
import java.util.TimeZone;

import javax.inject.Inject;

import org.apache.wink.guice.server.internal.lifecycle.WinkGuiceModule;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.data.Stat;
import org.eclipse.jetty.server.Server;
import org.keyczar.Crypter;
import org.keyczar.DefaultKeyType;
import org.keyczar.GenericKeyczar;
import org.keyczar.KeyMetadata;
import org.keyczar.enums.KeyPurpose;
import org.keyczar.exceptions.KeyczarException;
import org.platformlayer.metrics.MetricReporter;
import org.platformlayer.metrics.NullMetricsModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathomdb.Configuration;
import com.fathomdb.TimeSpan;
import com.fathomdb.config.ConfigurationImpl;
import com.fathomdb.crypto.EncryptionStore;
import com.fathomdb.discovery.ClasspathDiscovery;
import com.fathomdb.discovery.Discovery;
import com.fathomdb.extensions.Extensions;
import com.fathomdb.server.http.GuiceServletConfig;
import com.fathomdb.server.http.SslOption;
import com.fathomdb.server.http.WebServerBuilder;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.net.InetAddresses;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.persist.PersistService;

public class CloudServer {
    private static final Logger log = LoggerFactory.getLogger(CloudServer.class);

    static final int DEFAULT_HTTP_PORT = 8080;
    static final int DEFAULT_HTTPS_PORT = 8443;
    // static final int S3_PORT = 8082;

    @Inject
    WebServerBuilder serverBuilder;

    @Inject
    Lifecycle lifecycle;

    @Inject
    EncryptionStore encryptionStore;

    @Inject
    Configuration config;

    @Inject
    Configuration configuration;

    @Inject
    MetricReporter metricReporter;

    @Inject
    KeyczarFactory keyczarFactory;

    @Inject
    ServiceRegistration serviceRegistration;

    @Inject
    ZookeeperClient zk;

    private Server jettyServer;

    @Inject
    PersistService persistService;

    @Inject
    EmbeddedSshd sshd;

    @Inject
    GuiceServletConfig guiceServletConfig;

    // @Inject
    // RestEasyExternalInjector resteasy;

    public static void main(String[] args) throws Exception {
        // dockerTest();

        // Everything works better when we're in a sensible timezone
        // (in particular the default XML serialization)
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        System.setProperty("user.timezone", "UTC");

        /*
         * Removed until Zookeeper 3.5...
         * 
         * if (args.length != 0) { String command = args[0];
         * 
         * if (command.equals("join")) { try { JoinZookeeper join = new
         * JoinZookeeper(); join.join(args[1], args[2], args[3]); } catch
         * (Exception e) { e.printStackTrace(); System.exit(1); }
         * System.exit(0); } }
         */

        LogbackHook.attachToRootLogger();

        Configuration configuration = ConfigurationImpl.load();

        // TODO: Switch to ServiceLoadDiscovery
        Discovery discovery = new ClasspathDiscovery("com.fathomdb.", "io.fathom.");
        Extensions extensions = new Extensions(configuration, discovery);

        List<Module> modules = Lists.newArrayList();

        modules.add(new NullMetricsModule());

        modules.add(new DiscoveryModule(discovery));

        // modules.add(new ConfigurationModule());
        // modules.add(new CacheModule());

        // modules.add(new JdbcGuiceModule());
        modules.add(new FathomCloudGuiceModule(configuration));
        modules.add(new OpenstackServerServletModule(configuration, extensions));

        modules.add(new ZookeeperPersistModule());

        modules.add(new WinkGuiceModule());

        // modules.add(new AnnotationsModule());

        Injector injector = extensions.createInjector(configuration, modules);

        try {
            CloudServer server = injector.getInstance(CloudServer.class);
            // server.startZk();

            server.start();

        } catch (Exception e) {
            log.error("Error during startup", e);

            System.exit(1);
        }
    }

    // @Inject
    // CreatePasswordRecoveryKey cprk;

    private void start() throws Exception {
        waitForZk();

        persistService.start();

        createKeys();

        // {
        // cprk.path = IoUtils.resolve("~/passwordrecovery");
        // cprk.run();
        // }

        lifecycle.start();

        register();

        sshd.start();

        startHttp();
    }

    private void waitForZk() throws KeeperException, IOException {
        while (true) {
            try {
                Stat stat = zk.exists("/", false);
                return;
            } catch (KeeperException e) {
                if (e.code() == Code.CONNECTIONLOSS) {
                    log.warn("Cannot connect to zookeeper (no quorum?)");
                    TimeSpan.FIVE_SECONDS.doSafeSleep();
                } else {
                    throw new IOException("Error reading for zookeeper", e);
                }
            }
        }
    }

    private void register() throws CloudException {
        serviceRegistration.register();
    }

    private void createKeys() throws KeyczarException {
        {
            Crypter crypter = null;
            GenericKeyczar store = keyczarFactory.find(Secrets.KEY_TOKEN_ENCRYPT, crypter);
            if (store == null) {
                String nameFlag = "Authentication Token Encryption";

                KeyMetadata kmd = new KeyMetadata(nameFlag, KeyPurpose.DECRYPT_AND_ENCRYPT, DefaultKeyType.RSA_PRIV);
                store = keyczarFactory.create(Secrets.KEY_TOKEN_ENCRYPT, kmd, crypter);
            }
            keyczarFactory.ensureKeyCreated(store);
        }

        {
            Crypter crypter = null;
            GenericKeyczar store = keyczarFactory.find(SharedSecretTokenService.KEYSTORE_ID, crypter);
            if (store == null) {
                String nameFlag = "Authentication Token Signing";

                KeyMetadata kmd = new KeyMetadata(nameFlag, KeyPurpose.SIGN_AND_VERIFY, DefaultKeyType.HMAC_SHA1);
                store = keyczarFactory.create(SharedSecretTokenService.KEYSTORE_ID, kmd, crypter);
            }
            keyczarFactory.ensureKeyCreated(store);
        }

    }

    public void startHttp() throws Exception {
        InetAddress address = configuration.lookup("listen.address", (InetAddress) null);

        {
            int port = configuration.lookup("listen.http.port", DEFAULT_HTTP_PORT);
            serverBuilder.addHttpConnector(address, port, true);
        }

        if (encryptionStore.findCertificateAndKey("https") != null) {
            int port = configuration.lookup("listen.https.port", DEFAULT_HTTPS_PORT);
            EnumSet<SslOption> options = EnumSet.noneOf(SslOption.class);
            serverBuilder.addHttpsConnector(address, port, options);
        }

        // {
        // int metadataPort = configuration.lookup("metadata.port", 8775);
        // InetAddress metadataHost = configuration.lookup("metadata.host",
        // InetAddresses.forString("100.64.0.1"));
        // serverBuilder.addHttpConnector(metadataHost, metadataPort, true);
        // }

        {
            int metadataPort = configuration.lookup("metadata.port", 8775);
            InetAddress metadataHost = null;
            if (!Objects.equal("*", configuration.find("metadata.host"))) {
                metadataHost = configuration.lookup("metadata.host", InetAddresses.forString("fd00::feed"));
            }
            serverBuilder.addHttpConnector(metadataHost, metadataPort, true);
        }

        // {
        // int cloudPort = configuration.lookup("cloud.port", 8080);
        // InetAddress cloudHost = configuration.lookup("cloud.host",
        // InetAddresses.forString("fd00::c10d"));
        // serverBuilder.addHttpConnector(cloudHost, cloudPort, true);
        // }

        // guiceServletConfig.addServletContextListener(resteasy);

        serverBuilder.addGuiceContext("/", guiceServletConfig);

        // serverBuilder.enableRequestLogging();

        this.jettyServer = serverBuilder.start();
        // this.jettyServer.setSendServerVersion(false);

        metricReporter.start();
    }

    public void stop() throws Exception {
        if (jettyServer != null) {
            jettyServer.stop();
        }
    }
}
