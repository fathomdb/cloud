package io.fathom.cloud.compute.networks;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.compute.scheduler.SchedulerHost;
import io.fathom.cloud.compute.scheduler.SchedulerHost.SchedulerHostNetwork;
import io.fathom.cloud.compute.state.ComputeRepository;
import io.fathom.cloud.compute.state.NetworkStateStore;
import io.fathom.cloud.protobuf.CloudModel.InstanceData;
import io.fathom.cloud.protobuf.CloudModel.VirtualIpPoolData;
import io.fathom.cloud.server.model.Project;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Random;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

@Singleton
public class NetworkPools {

    private static final Logger log = LoggerFactory.getLogger(NetworkPools.class);

    @Inject
    NetworkStateStore networkStateStore;

    @Inject
    ComputeRepository repository;

    static final Random random = new Random();

    public NetworkPool buildPool(VirtualIpPoolData data) {
        switch (data.getType()) {
        case AMAZON_EC2:
            return new Ec2IpNetworkPool(this, data);
        case LAYER_3:
            return new MappableIpNetworkPool(this, data);
        default:
            throw new IllegalArgumentException();
        }
    }

    public NetworkPoolAllocation allocateIp(Project project, NetworkPool pool) throws CloudException {
        List<NetworkPool> pools = Lists.newArrayList();
        pools.add(pool);
        return allocateIps(project, pools).get(0);
    }

    public List<NetworkPoolAllocation> allocateIps(Project project, SchedulerHost host, InstanceData instance)
            throws CloudException {
        List<NetworkPool> pools = Lists.newArrayList();
        for (SchedulerHostNetwork network : host.getNetworks()) {
            NetworkPool pool = new HostNetworkPool(this, host, network, instance);
            pools.add(pool);
        }

        return allocateIps(project, pools);
    }

    private List<NetworkPoolAllocation> allocateIps(Project project, List<NetworkPool> pools) throws CloudException {
        for (int attempt = 0; attempt < 16; attempt++) {
            List<InetAddress> ips = Lists.newArrayList();
            List<NetworkPoolAllocation> allocated = Lists.newArrayList();

            // We try to align the addresses across the networks, no matter what
            // size they are
            // TODO: This ties us to the smallest network. We really should pick
            // that one first
            // TODO: We should actually let the smallest network generate the
            // random, and then continue
            long rand;
            synchronized (random) {
                rand = random.nextLong();
            }

            boolean ok = true;
            for (NetworkPool pool : pools) {
                InetAddress ip = pool.checkIpAvailable(rand);
                if (ip == null) {
                    log.warn("Concurrent creation while allocating IP; retrying");
                    ok = false;
                    break;
                }
                ips.add(ip);
            }

            if (!ok) {
                continue;
            }

            for (int i = 0; i < pools.size(); i++) {
                NetworkPool pool = pools.get(i);

                NetworkPoolAllocation alloc = pool.reserveIp(project, ips.get(i));
                if (alloc == null) {
                    for (NetworkPoolAllocation free : allocated) {
                        try {
                            free.releaseIp();
                        } catch (CloudException e) {
                            log.error("Error while releasing IPs", e);
                        }
                    }

                    log.warn("Error allocating ip due to concurrent activity");
                    ok = false;
                    break;
                }
                allocated.add(alloc);
            }

            if (ok) {
                return allocated;
            }
        }

        // Very suspicious...
        throw new IllegalStateException("Unable to allocate IP due to concurrent activity (despite retries)");
    }

    public static InetAddress toAddress(byte[] ip) {
        try {
            return InetAddress.getByAddress(ip);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException();
        }
    }
}
