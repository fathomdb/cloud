package io.fathom.cloud.openstack.client.loadbalance.model;

import io.fathom.cloud.openstack.client.SimpleRestClient;

import java.util.List;

public class LoadBalanceMappingList {
    public List<LbaasMapping> mappings;

    @Override
    public String toString() {
        return SimpleRestClient.asJson(this);
    }
}
