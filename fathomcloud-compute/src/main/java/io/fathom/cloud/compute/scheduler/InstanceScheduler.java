package io.fathom.cloud.compute.scheduler;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.compute.networks.IpRange;
import io.fathom.cloud.compute.scheduler.SchedulerHost.SchedulerHostNetwork;
import io.fathom.cloud.compute.services.ComputeSecrets;
import io.fathom.cloud.compute.services.DatacenterManager;
import io.fathom.cloud.compute.services.Ec2DatacenterManager;
import io.fathom.cloud.compute.services.RawDatacenterManager;
import io.fathom.cloud.compute.state.HostStore;
import io.fathom.cloud.protobuf.CloudModel.HostData;
import io.fathom.cloud.protobuf.CloudModel.HostGroupData;
import io.fathom.cloud.protobuf.CloudModel.HostGroupSecretData;
import io.fathom.cloud.protobuf.CloudModel.InstanceData;
import io.fathom.cloud.ssh.SshConfig;
import io.fathom.cloud.ssh.SshContext;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.persist.Transactional;

@Singleton
public class InstanceScheduler {
    private static final Logger log = LoggerFactory.getLogger(InstanceScheduler.class);

    @Inject
    HostStore hostStore;

    @Inject
    SshContext sshContext;

    @Inject
    ComputeSecrets computeSecrets;

    static final Random random = new Random();

    final Map<Long, SchedulerHost> allHosts = Maps.newHashMap();

    public SchedulerHost findHost(long i) {
        synchronized (allHosts) {
            return allHosts.get(i);
        }
    }

    // public SchedulerHost findHost(String key) {
    // synchronized (allHosts) {
    // for (SchedulerHost host : allHosts.values()) {
    // if (host.getKey().equals(key)) {
    // return host;
    // }
    // }
    // return null;
    // }
    // }

    @Transactional
    public void refreshHosts() throws CloudException {
        Map<Long, HostGroupData> hostGroups = Maps.newHashMap();
        Map<Long, DatacenterManager> datacenters = Maps.newHashMap();

        for (HostGroupData hostGroupData : hostStore.getHostGroups().list()) {
            hostGroups.put(hostGroupData.getId(), hostGroupData);
        }

        for (HostData hostData : hostStore.getHosts().list()) {
            HostGroupData hostGroupData = hostGroups.get(hostData.getHostGroup());
            if (hostGroupData == null) {
                throw new IllegalStateException();
            }

            DatacenterManager datacenter = datacenters.get(hostGroupData.getId());
            if (datacenter == null) {
                switch (hostGroupData.getHostGroupType()) {
                case HOST_GROUP_TYPE_RAW:
                    datacenter = new RawDatacenterManager();
                    break;
                case HOST_GROUP_TYPE_AMAZON_EC2:
                    HostGroupSecretData secretData = computeSecrets.getSecretData(hostGroupData);
                    datacenter = new Ec2DatacenterManager(hostGroupData, secretData);
                    break;
                default:
                    throw new IllegalStateException();
                }

                datacenters.put(hostGroupData.getId(), datacenter);
            }

            IpRange ipRange = IpRange.parse(hostData.getCidr());

            InetAddress address = ipRange.getAddress();

            InetSocketAddress sshSocketAddress = new InetSocketAddress(address, 22);
            SshConfig sshConfig = sshContext.buildConfig(sshSocketAddress);

            SchedulerHost host = new GawkerHost(datacenter, hostData, sshConfig);

            synchronized (allHosts) {
                allHosts.put(hostData.getId(), host);
            }
        }

        // TODO: Cope with deleted hosts
    }

    public List<SchedulerHost> pickHosts(SchedulerRequest request) {
        log.warn("Scheduler is tragically stupid");

        List<SchedulerHost> hosts = Lists.newArrayList();

        synchronized (allHosts) {
            List<Long> hostIds = Lists.newArrayList(allHosts.keySet());

            if (hostIds.size() == 0) {
                throw new IllegalArgumentException("No hosts are configured");
            }

            for (int i = 0; i < request.maxCount; i++) {
                int r;
                synchronized (random) {
                    r = random.nextInt(hostIds.size());
                }
                long hostId = hostIds.get(r);
                hosts.add(allHosts.get(hostId));
            }
        }

        return hosts;
    }

    public static class SchedulerResult {
        public InstanceData oldInstanceInfo;
    }

    public List<SchedulerHost> getAllHosts() {
        synchronized (allHosts) {
            List<SchedulerHost> hosts = Lists.newArrayList();
            hosts.addAll(allHosts.values());
            return hosts;
        }
    }

    public SchedulerHostNetwork findHostByAddress(InetAddress address) {
        // TODO: We really need to clean this up...
        synchronized (allHosts) {
            for (SchedulerHost host : allHosts.values()) {
                for (SchedulerHostNetwork network : host.getNetworks()) {
                    if (network.getIpRange().contains(address)) {
                        return network;
                    }
                }
            }
            return null;
        }
    }

}
