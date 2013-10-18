package io.fathom.cloud.cluster;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.protobuf.CloudCommons.NodeData;

import java.net.InetAddress;

import javax.inject.Inject;

import com.fathomdb.Configuration;
import com.google.common.net.InetAddresses;
import com.google.inject.persist.Transactional;

public class ServiceRegistrationImpl implements ServiceRegistration {
    @Inject
    ClusterService clusterService;

    @Inject
    Configuration config;

    @Inject
    LocalMachineInfo localMachineInfo;

    @Override
    @Transactional
    public void register() throws CloudException {
        registerStorageNode();
    }

    void registerStorageNode() throws CloudException {
        String key = localMachineInfo.getMachineKey();

        NodeData proposed = buildNodeConfig(key);
        clusterService.register(proposed);
    }

    private NodeData buildNodeConfig(String key) {
        NodeData.Builder node = NodeData.newBuilder();
        node.setKey(key);

        for (InetAddress address : localMachineInfo.getAddresses()) {
            if (address.isLoopbackAddress()) {
                continue;
            }

            if (address.isLinkLocalAddress()) {
                continue;
            }

            String s = InetAddresses.toAddrString(address);
            node.addAddress(s);
        }

        String queue = config.lookup("objectstore.queue", "sftp:///var/openstack/objectstore/queue/");
        node.setQueue(queue);

        String store = config.lookup("objectstore.store", "sftp:///var/openstack/objectstore/store/");
        node.setStore(store);

        NodeData proposed = node.build();
        return proposed;
    }
}
