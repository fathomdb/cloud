package io.fathom.auto.openstack.horizon;

import java.io.File;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

public class HorizonConfig {
    private static final Logger log = LoggerFactory.getLogger(HorizonConfig.class);

    final File instanceDir;

    public HorizonConfig(File instanceDir) {
        this.instanceDir = instanceDir;
    }

    public File getInstanceDir() {
        return instanceDir;
    }

    public String getIdentityUrl() {
        // TODO: Use https
        log.warn("Using http for API; switch to https");
        return "http://openstack:8080/openstack/identity/v2.0";
    }

    public Map<String, String> getHosts() {
        Map<String, String> hosts = Maps.newHashMap();
        hosts.put("openstack", "fd00::c10d");
        return hosts;
    }

    public File getInstallDir() {
        return new File("/opt/horizon");
    }
}
