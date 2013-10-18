package io.fathom.auto.endpoint;

import io.fathom.auto.JsonCodec;
import io.fathom.auto.config.ConfigEntry;
import io.fathom.auto.config.ConfigPath;
import io.fathom.auto.config.ConfigStore;
import io.fathom.auto.config.MachineInfo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

import com.google.common.collect.Lists;

public class EndpointRegistry {

    final ConfigPath endpointsRoot;

    public EndpointRegistry(ConfigStore configStore) {
        this.endpointsRoot = configStore.getSharedPath("endpoints");
    }

    public List<Endpoint> lookup(String serviceKey) {
        List<Endpoint> endpoints = Lists.newArrayList();

        try {
            ConfigPath parent = endpointsRoot.child(serviceKey);
            for (ConfigEntry child : parent.listChildren()) {
                String name = child.getName();
                String data = parent.readChild(name);

                EndpointData record = JsonCodec.gson.fromJson(data, EndpointData.class);

                Endpoint endpoint = new Endpoint(record);
                endpoints.add(endpoint);
            }

            return endpoints;
        } catch (IOException e) {
            // TODO: Retry??
            throw new IllegalArgumentException("Error reading endpoint catalog", e);
        }
    }

    public void register(String serviceKey, InetSocketAddress addr) {
        try {
            ConfigPath parent = endpointsRoot.child(serviceKey);

            EndpointData record = new EndpointData();
            record.address = InetSocketAddresses.toString(addr);

            String machineKey = MachineInfo.INSTANCE.getMachineKey();

            ConfigPath node = parent.child(machineKey);
            String json = JsonCodec.gson.toJson(record);
            node.write(json);
        } catch (IOException e) {
            // TODO: Retry??
            throw new IllegalArgumentException("Error writing endpoint entry", e);
        }
    }
}
