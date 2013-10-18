package io.fathom.auto;

import io.fathom.auto.haproxy.ConfigSync;
import io.fathom.cloud.openstack.client.loadbalance.model.LbaasMapping;
import io.fathom.cloud.openstack.client.loadbalance.model.LoadBalanceMappingList;

import java.io.File;
import java.io.IOException;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

public class StaticConfiguration implements ConfigSync {

    boolean dirty = true;
    final File mirrorPath;

    public StaticConfiguration(File mirrorPath) {
        this.mirrorPath = mirrorPath;
    }

    @Override
    public int firstSync() throws IOException {
        String key = "default";

        LbaasMapping target = new LbaasMapping();
        target.forwardUrl = null;
        target.host = null;
        target.ip = "127.0.0.1";
        target.key = key;
        target.port = 8080;

        LoadBalanceMappingList chunk = new LoadBalanceMappingList();
        chunk.mappings = Lists.newArrayList();
        chunk.mappings.add(target);

        String json = JsonCodec.gson.toJson(chunk);
        File file = new File(mirrorPath, key);
        Files.write(json, file, Charsets.UTF_8);

        return 0;
    }

    @Override
    public int refresh() throws IOException {
        return 0;
    }

    @Override
    public boolean isDirty() {
        return dirty;
    }

    @Override
    public void markClean() {
        dirty = false;
    }

}
