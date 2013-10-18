package io.fathom.auto.haproxy.model;

import io.fathom.auto.JsonCodec;

import java.util.List;

import com.google.common.collect.Lists;

public class Frontend {
    public String key;
    public List<Backend> backends = Lists.newArrayList();
    public String sslKey;

    @Override
    public String toString() {
        return JsonCodec.formatJson(this);
    }
}