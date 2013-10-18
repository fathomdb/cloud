package io.fathom.cloud.compute.actions;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.compute.networks.VirtualIp;
import io.fathom.cloud.compute.scheduler.SchedulerHost;
import io.fathom.cloud.protobuf.CloudModel.InstanceData;
import io.fathom.cloud.protobuf.CloudModel.NetworkAddressData;
import io.fathom.cloud.protobuf.CloudModel.VirtualIpData;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.net.InetAddresses;

public class ConfigureVirtualIp {
    private final ApplydContext applyd;
    private final SchedulerHost host;

    public ConfigureVirtualIp(SchedulerHost host, ApplydContext applyd) {
        super();
        this.host = host;
        this.applyd = applyd;
    }

    public boolean updateConfig(InstanceData instance, VirtualIp vip, String hostIp) throws CloudException {
        boolean dirty = false;

        String key = getKey(vip);

        dirty |= applyd.updateConfig("iptables/50-" + "os-compute-vip-" + key,
                buildIptables(instance, vip.getData(), hostIp, false));
        // dirty |= applyd.updateConfig("arp/os-compute-vip-" + key,
        // buildArp(instance, vip, true));

        dirty |= applyd
                .updateConfig("vips/" + vip.getData().getIp(), buildIpAssignment(instance, vip.getData(), false));

        return dirty;
    }

    private String getKey(VirtualIp vip) {
        return vip.getPoolData().getId() + "-" + vip.getData().getIp().replace('.', '_').replace(':', '_');
    }

    public boolean removeConfig(VirtualIp vip) throws CloudException {
        boolean dirty = false;

        String key = getKey(vip);

        dirty |= applyd.removeConfig("iptables/50-" + "os-compute-vip-" + key);
        // dirty |= applyd.removeConfig("arp/os-compute-vip-" + key);

        dirty |= applyd.updateConfig("vips/" + vip.getData().getIp(), "");

        return dirty;
    }

    private String buildIptables(InstanceData instance, VirtualIpData vipData, String hostIp, boolean ipv6) {
        StringBuilder conf = new StringBuilder();

        List<String> ips = Lists.newArrayList();

        for (NetworkAddressData addressInfo : instance.getNetwork().getAddressesList()) {
            InetAddress address = InetAddresses.forString(addressInfo.getIp());
            boolean isIpv6 = address instanceof Inet6Address;
            if (isIpv6 != ipv6) {
                continue;
            }

            ips.add(InetAddresses.toAddrString(address));
        }

        if (ips.size() == 0) {
            throw new IllegalStateException("Cannot find any IPs for VIP redirect");
        }

        if (ips.size() != 1) {
            throw new IllegalStateException("Found multiple IPs for VIP redirect");
        }

        String ip = ips.get(0);

        conf.append("*nat\n");

        // We remap all traffic, replacing the default NAT rule
        // We do this because otherwise we have to be stateful, which sucks
        conf.append(String.format("-A PREROUTING -d %s -j DNAT --to-destination %s\n", hostIp, ip));
        conf.append(String.format("-A POSTROUTING -s %s -j SNAT --to-source %s\n", ip, hostIp));

        conf.append("COMMIT\n");

        return conf.toString();
    }

    private String buildIpAssignment(InstanceData instance, VirtualIpData vipData, boolean ipv6) {
        StringBuilder conf = new StringBuilder();

        String networkDevice = host.getNetworkDevice();

        String vip = vipData.getIp();

        // TODO: We should try to make sure that applyd doesn't do this on
        // reboot without double-checking (or at least doesn't do an ARP
        // broadcast!)
        // conf.append(String.format("ip addr add %s/32 dev %s\n", vip,
        // networkDevice));
        conf.append(String.format("%s %s\n", networkDevice, vip));

        return conf.toString();
    }
}
