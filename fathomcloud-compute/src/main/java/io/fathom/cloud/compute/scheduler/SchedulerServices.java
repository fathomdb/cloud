package io.fathom.cloud.compute.scheduler;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.compute.networks.HostNetworkPool;
import io.fathom.cloud.compute.networks.NetworkPoolAllocation;
import io.fathom.cloud.compute.networks.NetworkPools;
import io.fathom.cloud.compute.state.ComputeRepository;
import io.fathom.cloud.protobuf.CloudModel.InstanceData;
import io.fathom.cloud.protobuf.CloudModel.NetworkAddressData;
import io.fathom.cloud.server.model.Project;
import io.fathom.cloud.state.NumberedItemCollection;

import java.util.List;
import java.util.Random;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.persist.Transactional;

@Singleton
@Transactional
public class SchedulerServices {

    private static final Logger log = LoggerFactory.getLogger(SchedulerServices.class);

    @Inject
    private ComputeRepository computeStore;

    @Inject
    NetworkPools networkPools;

    public InstanceData updateInstance(final InstanceData instance, final InstanceData.Builder changes)
            throws CloudException {
        NumberedItemCollection<InstanceData> instances = computeStore.getInstances(instance.getProjectId());
        InstanceData read = instances.find(instance.getId());
        InstanceData.Builder b = InstanceData.newBuilder(read);
        b.mergeFrom(changes.build());
        return instances.update(b);
    }

    private String buildMac() {
        // TODO: We should verify that this is in fact unique!

        // See here for a discussion of the first byte:
        // https://bugs.launchpad.net/nova/+bug/921838

        Random random = new Random();

        int b0 = 0xfa;
        int b1 = random.nextInt(256);
        int b2 = random.nextInt(256);
        int b3 = random.nextInt(256);
        int b4 = random.nextInt(256);
        int b5 = random.nextInt(256);

        String mac = String.format("%02X:%02X:%02X:%02X:%02X:%02X", b0, b1, b2, b3, b4, b5);
        return mac;
    }

    public InstanceData assignIps(Project project, SchedulerHost host, InstanceData instance) throws CloudException {
        List<NetworkPoolAllocation> addresses = networkPools.allocateIps(project, host, instance);

        InstanceData.Builder i = InstanceData.newBuilder(instance);

        for (NetworkPoolAllocation address : addresses) {
            NetworkAddressData.Builder addressData = NetworkAddressData
                    .newBuilder(((HostNetworkPool.Allocation) address).getData());
            addressData.setMacAddress(buildMac());

            i.getNetworkBuilder().addAddresses(addressData);
        }

        return updateInstance(instance, i);
    }
}
