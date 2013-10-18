package io.fathom.cloud.compute.actions;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.compute.scheduler.SchedulerHost;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;

public class ConfigureIpset {
    private final ApplydContext applyd;
    private final SchedulerHost host;

    public ConfigureIpset(SchedulerHost host, ApplydContext applyd) {
        this.host = host;
        this.applyd = applyd;
    }

    public boolean updateConfig(long securityGroupId, Set<String> ipset) throws CloudException {
        boolean dirty = false;

        List<String> ips = Lists.newArrayList(ipset);
        Collections.sort(ips);

        String name = "sg-" + securityGroupId;
        StringBuilder s = new StringBuilder();
        s.append("create " + name + " hash:ip family inet6 hashsize 1024 maxelem 65536\n");
        for (String ip : ips) {
            s.append("add " + name + " " + ip + "\n");
        }

        dirty |= applyd.updateConfig("ipset/" + name, s.toString());

        return dirty;
    }

}
