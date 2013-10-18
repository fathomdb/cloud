package io.fathom.cloud.server;

import io.fathom.cloud.zookeeper.ZookeeperClient;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathomdb.Configuration;
import com.fathomdb.config.ConfigurationBase;
import com.fathomdb.properties.PropertyUtils;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.name.Named;

@Singleton
public class ClusterConfiguration extends ConfigurationBase {
    private static final Logger log = LoggerFactory.getLogger(ClusterConfiguration.class);

    private final Configuration instanceConfig;
    private final ZookeeperClient zookeeperClient;

    Map<String, String> propertyMap;

    @Inject
    public ClusterConfiguration(@Named("instance") Configuration instanceConfig, ZookeeperClient zookeeperClient) {
        this.instanceConfig = instanceConfig;
        this.zookeeperClient = zookeeperClient;
    }

    @Override
    public String lookup(String key, String defaultValue) {
        String value = instanceConfig.find(key);
        if (value == null) {
            value = getPropertyMap().get(key);
        } else {
            if (propertyMap.containsKey(key)) {
                log.warn("Cluster config and instance config both contained key (choosing instance config): {}", key);
            }
        }

        if (value != null) {
            return value;
        }

        return defaultValue;
    }

    @Override
    public File getBasePath() {
        return instanceConfig.getBasePath();
    }

    @Override
    public Configuration getChildTree(String prefix) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> getKeys() {
        Set<String> keys = Sets.newHashSet();

        keys.addAll(instanceConfig.getKeys());

        keys.addAll(getPropertyMap().keySet());

        return keys;
    }

    private synchronized Map<String, String> getPropertyMap() {
        if (propertyMap == null) {
            try {
                byte[] data = null;

                try {
                    Stat stat = new Stat();
                    data = zookeeperClient.getData("/config", null, stat);
                } catch (NoNodeException e) {
                    // Ignore
                    log.debug("No cluster config node data");
                } catch (KeeperException e) {
                    throw new IllegalArgumentException("Error loading config from zookeeper", e);
                }

                Properties properties = new Properties();
                if (data != null) {
                    properties.load(new ByteArrayInputStream(data));
                }

                Map<String, String> propertyMap = Maps.newHashMap();
                PropertyUtils.copyToMap(properties, propertyMap);
                this.propertyMap = propertyMap;
            } catch (IOException e) {
                throw new IllegalArgumentException("Error loading config from zookeeper", e);
            }
        }
        return propertyMap;
    }

}
