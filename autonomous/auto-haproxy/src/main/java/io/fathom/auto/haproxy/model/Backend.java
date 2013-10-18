package io.fathom.auto.haproxy.model;

import io.fathom.auto.JsonCodec;
import io.fathom.cloud.openstack.client.loadbalance.model.LbaasMapping;

import java.util.List;

import com.google.common.collect.Lists;

public class Backend {
    public String host;
    public String key;

    public String sslKey;

    public List<LbaasMapping> mappings = Lists.newArrayList();

    @Override
    public String toString() {
        return JsonCodec.formatJson(this);
    }
}
