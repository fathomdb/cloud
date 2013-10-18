package io.fathom.auto.config;

import io.fathom.auto.TimeSpan;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

public abstract class ConfigStore {
    public static final String KEY_DNS_HOST = "__dns:host";

    private static final Logger log = LoggerFactory.getLogger(ConfigStore.class);

    ConfigPath root;

    protected ConfigStore() {
    }

    public abstract void init();

    static final Map<String, String> WELL_KNOWN_PROVIDERS;

    static {
        WELL_KNOWN_PROVIDERS = Maps.newLinkedHashMap();
        WELL_KNOWN_PROVIDERS.put("etcd", "io.fathom.auto.config.EtcdConfigStoreProvider");
        WELL_KNOWN_PROVIDERS.put("openstack", "io.fathom.auto.config.OpenstackConfigStoreProvider");
    }

    public static ConfigStore get(String serviceKey) {
        while (true) {
            try {
                String configStoreProperty = System.getProperty("configStore");

                String configClass = null;

                Collection<String> classes = WELL_KNOWN_PROVIDERS.values();

                if (configStoreProperty != null) {
                    configClass = WELL_KNOWN_PROVIDERS.get(configStoreProperty);
                    if (configClass == null) {
                        // Assume it's a class name
                        configClass = configStoreProperty;
                    }
                    classes = Collections.singletonList(configClass);
                }

                for (String className : classes) {
                    ConfigStoreProvider configStoreProvider = null;

                    try {
                        Class<?> providerClass = Class.forName(className);
                        configStoreProvider = (ConfigStoreProvider) providerClass.newInstance();
                    } catch (Exception e) {
                        log.warn("Error loading config provider: " + className, e);
                    }

                    ConfigStore config = null;

                    if (configStoreProvider != null) {
                        config = get(configStoreProvider, serviceKey);
                    }

                    if (config != null) {
                        config.init();
                        return config;
                    }
                }

                log.warn("Unable to determine config; waiting and retrying");
            } catch (Exception e) {
                log.warn("Error buildling configuration store", e);
            }
            TimeSpan.seconds(5).sleep();
        }
    }

    public static ConfigStore get(ConfigStoreProvider configStoreProvider, String serviceKey) {
        ConfigStore config = null;

        try {
            if (configStoreProvider.init()) {
                String clusterKey = "__default";

                String basePath = configStoreProvider.getInstanceProperty("configStorePath");
                if (basePath == null) {
                    // TODO: Support multiple clusters?
                    basePath = clusterKey + "/";
                } else {
                    if (!basePath.endsWith("/")) {
                        basePath += "/";
                    }
                }

                try {
                    config = configStoreProvider.build(clusterKey, basePath, serviceKey);
                } catch (Exception e) {
                    log.warn("Error loading config from: " + configStoreProvider.getClass().getSimpleName(), e);
                }
            }

            if (config != null) {
                config.init();
                return config;
            }

            log.warn("Unable to determine config; waiting and retrying");
        } catch (Exception e) {
            log.warn("Error buildling configuration store", e);
        }

        return config;
    }

    public static abstract class ConfigStoreProvider {
        protected abstract ConfigStore build(String clusterKey, String basePath, String serviceKey);

        public abstract String getInstanceProperty(String key);

        public abstract boolean init();
    }

    public synchronized ConfigPath getConfigRoot() {
        if (root == null) {
            root = getConfigRoot0();
        }
        return root;
    }

    protected abstract ConfigPath getConfigRoot0();

    public abstract ConfigPath getSharedPath(String key);

    public abstract String getClusterKey();

    public abstract SecretKeys getSecretKeys();

    public abstract String getInstanceProperty(String key);
}
