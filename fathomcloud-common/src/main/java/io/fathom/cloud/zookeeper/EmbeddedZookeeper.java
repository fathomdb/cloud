package io.fathom.cloud.zookeeper;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;

import javax.inject.Inject;

import org.apache.zookeeper.server.DatadirCleanupManager;
import org.apache.zookeeper.server.quorum.QuorumPeer;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig.ConfigException;
import org.apache.zookeeper.server.quorum.QuorumPeerMain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathomdb.Configuration;
import com.fathomdb.TimeSpan;
import com.fathomdb.io.IoUtils;
import com.google.common.net.InetAddresses;
import com.google.inject.name.Named;

public class EmbeddedZookeeper implements ZookeeperCluster {

    private static final Logger log = LoggerFactory.getLogger(EmbeddedZookeeper.class);

    // int maxClientCnxns = 5000;
    // int tickTime = 2000;
    // final InetAddress address;
    // final int port;

    InetSocketAddress clientPortAddress;

    final File baseDir;

    // ZooKeeperServer server;
    // private final int initLimit = 5;
    // private final int syncLimit = 2;

    @Inject
    public EmbeddedZookeeper(@Named("instance") Configuration config) throws IOException, ConfigException,
            InterruptedException {
        this.baseDir = IoUtils.resolve(config.get("zookeeper.embedded.basedir"));
    }

    public void start() {
        final File confFile = new File(baseDir, "zk.conf");

        final ReuseQuorumPeerMain quorumPeerMain = new ReuseQuorumPeerMain();

        Thread serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    quorumPeerMain.run(confFile);
                    log.warn("Zookeeper exited");
                } catch (Throwable t) {
                    log.error("Error starting zookeeper", t);
                }
            }
        });

        serverThread.start();

        while (true) {
            QuorumPeerConfig config = quorumPeerMain.getConfig();
            if (config != null) {
                this.clientPortAddress = config.getClientPortAddress();
                if (this.clientPortAddress == null) {
                    QuorumPeer quorumPeer = quorumPeerMain.getQuorumPeer();
                    if (quorumPeer != null) {
                        this.clientPortAddress = quorumPeer.getClientAddress();
                    }
                }
            }

            if (this.clientPortAddress != null) {
                break;
            }

            log.info("Waiting for embedded zookeeper server to start");
            TimeSpan.ONE_SECOND.doSafeSleep();
        }
        // this.address = config.lookup("listen.address", (InetAddress) null);
        // this.port = config.lookup("zookeeper.embedded.port", 2181);
        //
        // File leaders = new File(baseDir, "leaders.conf");
        // Properties zkQuorumProperties = new Properties();
        //
        // if (leaders.exists()) {
        // try (FileInputStream fis = new FileInputStream(leaders)) {
        // zkQuorumProperties.load(fis);
        // }
        // }
        //
        // if (!zkQuorumProperties.isEmpty()) {
        // startZookeeperQuorum(baseDir, zkQuorumProperties);
        // } else {
        // // startZookeeperQuorum(baseDir, zkQuorumProperties);
        // startZookeeperStandalone(baseDir);
        // }
    }

    // void startZookeeperStandalone(File baseDir) throws IOException,
    // InterruptedException {
    // File snapDir = new File(baseDir, "snapshots");
    // File logDir = new File(baseDir, "logs");
    //
    // server = new ZooKeeperServer(snapDir, logDir, tickTime);
    // ServerCnxnFactory standaloneServerFactory = ServerCnxnFactory
    // .createFactory();
    // standaloneServerFactory.configure(new InetSocketAddress(address, port),
    // maxClientCnxns);
    //
    // standaloneServerFactory.startup(server); // start the server.
    //
    // // // Note that this thread isn't going to be doing anything else,
    // // // so rather than spawning another thread, we will just call
    // // // run() in this thread.
    // // // create a file logger url from the command line args
    // // ZooKeeperServer zkServer = new ZooKeeperServer( new
    // // FileTxnSnapLog(config.dataLogDir, config.dataDir),
    // // config.tickTime, config.minSessionTimeout, config.maxSessionTimeout,
    // // null);
    // //
    // // cnxnFactory = ServerCnxnFactory.createFactory();
    // // cnxnFactory.configure(config.getClientPortAddress(),
    // // config.getMaxClientCnxns());
    // // cnxnFactory.startup(zkServer);
    // // cnxnFactory.join();
    // // if (zkServer.isRunning()) {
    // // zkServer.shutdown();
    // // }
    //
    // }

    static class ReuseQuorumPeerMain extends QuorumPeerMain {

        public void run(File conf) throws ConfigException, IOException {
            initializeAndRun(new String[] { conf.getAbsolutePath() });
        }

        private QuorumPeerConfig config;

        @Override
        public void runFromConfig(QuorumPeerConfig config) throws IOException {
            this.config = config;
            super.runFromConfig(config);
        }

        @Override
        protected void initializeAndRun(String[] args) throws ConfigException, IOException {
            QuorumPeerConfig config = new QuorumPeerConfig();
            if (args.length == 1) {
                config.parse(args[0]);
            }

            // Start and schedule the the purge task
            DatadirCleanupManager purgeMgr = new DatadirCleanupManager(config.getDataDir(), config.getDataLogDir(),
                    config.getSnapRetainCount(), config.getPurgeInterval());
            purgeMgr.start();

            if (args.length == 1 && config.isDistributed()) {
                runFromConfig(config);
            } else {
                runFromConfig(config);
                // LOG.warn("Either no config or no quorum defined in config, running "
                // + " in standalone mode");
                // // there is only server in the quorum -- run as standalone
                // ZooKeeperServerMain.main(args);
            }
        }

        public QuorumPeerConfig getConfig() {
            return config;
        }

        public QuorumPeer getQuorumPeer() {
            return quorumPeer;
        }

    }

    // void startZookeeperQuorum(File baseDir, Properties zkQuorumProperties)
    // throws IOException, ConfigException {
    //
    // // This is a copy of the code in QuorumPeerMain ... ideally we would
    // // reuse this...
    // File snapDir = new File(baseDir, "snapshots");
    // File logDir = new File(baseDir, "logs");
    //
    // final QuorumPeerConfig config = new QuorumPeerConfig();
    // InetSocketAddress clientPortAddress = new InetSocketAddress(address,
    // port);
    //
    // Properties properties = new Properties();
    // properties.putAll(zkQuorumProperties);
    //
    // properties.put("dataLogDir", logDir.getAbsolutePath());
    // properties.put("dataDir", snapDir.getAbsolutePath());
    // properties.put("clientPort", port);
    // properties.put("clientPortAddress", clientPortAddress.getAddress()
    // .getHostAddress());
    // properties.put("tickTime", tickTime);
    // properties.put("maxClientCnxns", maxClientCnxns);
    //
    // properties.put("initLimit", initLimit);
    // properties.put("syncLimit", syncLimit);
    //
    // properties.put("electionAlg", 3);
    //
    // // minSessionTimeout
    // // maxSessionTimeout
    // // electionAlg
    // // peerType
    //
    // String myid = null;
    //
    // {
    // File myIdFile = new File(snapDir, "myid");
    // if (myIdFile.exists()) {
    // myid = Files.toString(myIdFile, Charsets.UTF_8);
    // }
    // }
    //
    // // if (myid != null) {
    // // properties.getProperty("server." + myid);
    // // }
    //
    // config.parseProperties(properties);
    //
    // // Start and schedule the the purge task
    // DatadirCleanupManager purgeMgr = new DatadirCleanupManager(
    // config.getDataDir(), config.getDataLogDir(),
    // config.getSnapRetainCount(), config.getPurgeInterval());
    // purgeMgr.start();
    //
    // // ServerCnxnFactory cnxnFactory = ServerCnxnFactory.createFactory();
    // // cnxnFactory.configure(config.getClientPortAddress(),
    // // config.getMaxClientCnxns());
    //
    // Thread serverThread = new Thread(new Runnable() {
    // @Override
    // public void run() {
    // try {
    // ReuseQuorumPeerMain quorumPeerMain = new ReuseQuorumPeerMain();
    // quorumPeerMain.run(confFile);
    // } catch (Throwable t) {
    // log.error("Error starting zookeeper", t);
    // }
    // }
    // });
    //
    // serverThread.start();
    // }

    // public void stop() {
    // server.shutdown();
    // }

    public String getClientConnectString() {
        return InetAddresses.toAddrString(clientPortAddress.getAddress()) + ":" + clientPortAddress.getPort();
    }

    @Override
    public ZookeeperClient getZookeeperClient() {
        synchronized (this) {
            if (clientPortAddress == null) {
                this.start();
            }
        }

        String connectString = getClientConnectString();

        return new ZookeeperClient(connectString);
    }
}
