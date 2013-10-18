package io.fathom.cloud.zookeeper;

import java.io.IOException;

import javax.inject.Inject;

import org.apache.zookeeper.server.quorum.QuorumPeerConfig.ConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathomdb.Configuration;
import com.google.inject.name.Named;

public class ExternalZookeeper implements ZookeeperCluster {
    private static final Logger log = LoggerFactory.getLogger(ExternalZookeeper.class);

    private final String clientConnectString;

    @Inject
    public ExternalZookeeper(@Named("instance") Configuration config) throws IOException, ConfigException,
            InterruptedException {
        this.clientConnectString = config.get("zookeeper.servers");
    }

    public void start() {
    }

    @Override
    public ZookeeperClient getZookeeperClient() {
        return new ZookeeperClient(clientConnectString);
    }
}
