package io.fathom.auto.zookeeper;

import io.fathom.auto.TimeSpan;
import io.fathom.auto.config.ConfigPath;
import io.fathom.auto.zookeeper.model.ClusterState;
import io.fathom.auto.zookeeper.model.ZookeeperClusterRegistration;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZookeeperInstance {
    private static final Logger log = LoggerFactory.getLogger(ZookeeperInstance.class);

    File baseDir;

    ConfigPath lockPath;

    private final ZookeeperCluster cluster;

    public ZookeeperInstance(ConfigPath configPath) {
        ConfigPath clusterPath = configPath.child("cluster");
        ConfigPath lockPath = configPath.child("lock");

        this.cluster = new ZookeeperCluster(clusterPath, lockPath.buildLock());
    }

    public void run() throws IOException {
        ZookeeperClusterRegistration registration = cluster.register();

        int myid = registration.serverId;

        File installDir = new File("/opt/zookeeper");
        File instanceDir = new File("/var/zookeeper");
        ZookeeperConfig config = new ZookeeperConfig(installDir, instanceDir);

        config.mkdirs();

        {
            Integer savedId = config.readIdFile();

            if (savedId == null) {
                config.writeIdFile(myid);
            } else {
                if (savedId != myid) {
                    throw new IllegalStateException("myid does not match");
                }
            }
        }

        final SupervisedZookeeper zk = SupervisedZookeeper.build(config);

        while (!zk.isRunning()) {
            log.info("Checking zookeeper cluster creation state");

            try {
                ClusterState clusterState = cluster.readClusterState();
                if (clusterState != null && clusterState.isCreated()) {
                    log.info("Zookeeper cluster is created");
                    break;
                } else {
                    log.info("Zookeeper cluster state: {}", clusterState);
                }
            } catch (IOException e) {
                log.warn("Error reading cluster state", e);
                TimeSpan.seconds(10).sleep();
                continue;
            }

            try {
                if (cluster.getLock().tryLock(1, TimeUnit.MINUTES)) {
                    // We've got the lock...
                    ClusterSnapshot snapshot = cluster.getSnapshot();

                    ClusterState state = snapshot.getClusterState();
                    if (state == null || !state.isCreated()) {
                        // If we haven't created the data yet, everybody should
                        // be an observer
                        for (ZookeeperClusterRegistration server : snapshot.servers.values()) {
                            if (!server.isObserver()) {
                                // TODO: It may be that the leader crashed
                                // mid-create...
                                throw new IllegalStateException("Found non-observer server in uninitialized cluster");
                            }
                        }

                        registration.type = ZookeeperClusterRegistration.PARTICIPANT;
                        cluster.writeRegistration(myid, registration);

                        snapshot.servers.put(myid, registration);

                        config.writeHosts(snapshot);
                        config.writeConfig(snapshot, myid);

                        zk.start();

                        if (state == null) {
                            state = new ClusterState();
                        }
                        state.createdBy = String.valueOf(myid);

                        cluster.writeClusterState(state);
                    }
                } else {
                    log.info("Unable to obtain lock; will retry");
                }
            } catch (Exception e) {
                log.warn("Error initializing cluster", e);
                cluster.getLock().unlock();
                TimeSpan.seconds(10).sleep();
                continue;
            } finally {
                cluster.getLock().unlock();
            }
        }

        if (!zk.isRunning()) {
            ClusterSnapshot snapshot = cluster.getSnapshot();

            config.writeHosts(snapshot);
            config.writeConfig(snapshot, myid);

            log.info("Starting zookeeper");

            zk.start();
        }

        zk.monitor();
    }

    // private static void startZk(EmbeddedZookeeper zk) {
    // zk.start();
    //
    // while (true) {
    // try {
    // if (zk.isAlive()) {
    // break;
    // }
    // log.info("Waiting for embedded zookeeper server to start");
    // } catch (Exception e) {
    // log.error("Error waiting for zookeeper server to start", e);
    // }
    //
    // TimeSpan.ONE_SECOND.doSafeSleep();
    // }
    // }

}
