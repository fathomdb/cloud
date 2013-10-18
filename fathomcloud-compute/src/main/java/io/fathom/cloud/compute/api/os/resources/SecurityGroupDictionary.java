package io.fathom.cloud.compute.api.os.resources;

import io.fathom.cloud.protobuf.CloudModel.SecurityGroupData;

import java.util.Map;

import com.google.common.collect.Maps;

public class SecurityGroupDictionary {
    final Map<String, SecurityGroupData> byName = Maps.newHashMap();
    final Map<Long, SecurityGroupData> byId = Maps.newHashMap();

    void add(SecurityGroupData data) {
        String name = data.getName();
        long id = data.getId();

        byName.put(name, data);
        byId.put(id, data);
    }

    public SecurityGroupDictionary(Iterable<SecurityGroupData> groups) {
        for (SecurityGroupData data : groups) {
            add(data);
        }
    }

    public SecurityGroupData getById(long id) {
        return byId.get(id);
    }

    public SecurityGroupData getByName(String name) {
        return byName.get(name);
    }
}
