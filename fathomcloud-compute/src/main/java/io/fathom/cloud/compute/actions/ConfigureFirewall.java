package io.fathom.cloud.compute.actions;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.compute.networks.IpRange;
import io.fathom.cloud.compute.scheduler.SchedulerHost;
import io.fathom.cloud.protobuf.CloudModel.InstanceData;
import io.fathom.cloud.protobuf.CloudModel.NetworkAddressData;
import io.fathom.cloud.protobuf.CloudModel.Protocols;
import io.fathom.cloud.protobuf.CloudModel.SecurityGroupData;
import io.fathom.cloud.protobuf.CloudModel.SecurityGroupRuleData;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.net.InetAddresses;

public class ConfigureFirewall {
    private static final Logger log = LoggerFactory.getLogger(ConfigureFirewall.class);

    private final ApplydContext applyd;
    private final SchedulerHost host;

    public ConfigureFirewall(SchedulerHost host, ApplydContext applyd) {
        super();
        this.host = host;
        this.applyd = applyd;
    }

    public boolean updateConfig(InstanceData instance) throws CloudException {
        boolean dirty = false;

        dirty |= applyd.updateConfig("iptables/50-" + "os-compute-inst-" + instance.getId(), build(instance, false));
        dirty |= applyd.updateConfig("ip6tables/50-" + "os-compute-inst-" + instance.getId(), build(instance, true));

        dirty |= applyd.updateConfig("ip6neigh/os-compute-inst-" + instance.getId(), buildIpNeighProxy(instance, true));

        // Tunnel names are of limited length
        dirty |= applyd.updateConfig("tunnel/i-" + Long.toHexString(instance.getId()), buildTunnel(instance));

        return dirty;
    }

    public boolean removeConfig(InstanceData instance) throws CloudException {
        boolean dirty = false;

        dirty |= applyd.removeConfig("iptables/50-" + "os-compute-inst-" + instance.getId());
        dirty |= applyd.removeConfig("ip6tables/50-" + "os-compute-inst-" + instance.getId());

        dirty |= applyd.removeConfig("ip6neigh/os-compute-inst-" + instance.getId());

        dirty |= applyd.removeConfig("tunnel/os-compute-inst-" + instance.getId());

        return dirty;
    }

    public boolean updateConfig(SecurityGroupData sg) throws CloudException {
        boolean dirty = false;

        dirty |= applyd.updateConfig("iptables/50-" + "os-compute-sg-" + sg.getId(), build(sg, false));
        dirty |= applyd.updateConfig("ip6tables/50-" + "os-compute-sg-" + sg.getId(), build(sg, true));

        return dirty;
    }

    private String build(SecurityGroupData sg, boolean ipv6) {
        StringBuilder conf = new StringBuilder();

        // e.g.
        // *filter
        // -A INPUT -m state --state ESTABLISHED -j ACCEPT
        // COMMIT

        conf.append("*filter\n");
        conf.append(":" + "os-compute-sg-" + sg.getId() + " -\n");

        for (SecurityGroupRuleData rule : sg.getRulesList()) {
            String prefix = "-A os-compute-sg-" + sg.getId();

            String filt = "";
            if (rule.hasFromCidr()) {
                IpRange range = Instance.toIpRange(rule.getFromCidr());

                if (range.isIpv6() != ipv6) {
                    continue;
                }

                filt += " -s " + InetAddresses.toAddrString(range.getAddress()) + "/" + range.getNetmaskLength();
            }

            if (rule.hasFromSecurityGroup()) {
                String setKey = "sg-" + rule.getFromSecurityGroup();
                filt += "-m set --match-set " + setKey + "  src";
            }

            if (rule.getIpProtocolCount() != 0) {
                if (rule.getIpProtocolCount() != 1) {
                    throw new UnsupportedOperationException();
                }

                for (int ipProtocol : rule.getIpProtocolList()) {
                    switch (ipProtocol) {
                    case Protocols.ICMP_VALUE:
                        filt += " -p icmp";
                        break;
                    case Protocols.UDP_VALUE:
                        filt += " -p udp";
                        break;
                    case Protocols.TCP_VALUE:
                        filt += " -p tcp -m tcp";
                        break;
                    default:
                        throw new IllegalStateException();
                    }
                }
            }

            if (rule.hasFromPortLow()) {
                if (!rule.hasFromPortHigh() || rule.getFromPortHigh() == rule.getFromPortLow()) {
                    if (rule.getFromPortLow() > 0) {
                        filt += " --dport " + rule.getFromPortLow();
                    } else {
                        log.warn("Ignoring invalid port range: {}", rule);
                    }
                } else {
                    filt += " --dport " + rule.getFromPortLow() + ":" + rule.getFromPortHigh();
                }
            }

            conf.append(prefix + " " + filt + " -j ACCEPT\n");
        }

        conf.append("COMMIT\n");

        return conf.toString();
    }

    private String build(InstanceData instance, boolean ipv6) {
        StringBuilder conf = new StringBuilder();

        List<String> ips = findIps(instance, ipv6);

        // *filter
        // -A INPUT -m state --state ESTABLISHED -j ACCEPT
        // COMMIT

        conf.append("*filter\n");

        {
            // os-compute-local chain

            String prefix = "-A os-compute-local ";

            // Jump to a per-instance rule if the target IP matches
            for (String ip : ips) {
                conf.append(prefix + "-d " + ip + " -j os-compute-inst-" + instance.getId() + "\n");
            }
        }

        {
            // os-compute-inst-<id>

            String prefix = "-A os-compute-inst-" + instance.getId() + " ";

            conf.append(prefix + "-m state --state INVALID -j DROP\n");
            conf.append(prefix + "-m state --state RELATED,ESTABLISHED -j ACCEPT\n");

            for (long securityGroupId : instance.getSecurityGroupIdList()) {
                String sgRuleName = "os-compute-sg-" + securityGroupId;
                conf.append(prefix + "-j " + sgRuleName + "\n");
            }

            conf.append(prefix + "-j os-compute-sg-fallback\n");
        }

        conf.append("COMMIT\n");

        return conf.toString();
    }

    private String buildIpNeighProxy(InstanceData instance, boolean ipv6) {
        StringBuilder conf = new StringBuilder();

        List<String> ips = findIps(instance, ipv6);

        {
            String prefix = "ip -6 neigh add proxy ";
            String suffix = " dev " + host.getNetworkDevice();

            for (String ip : ips) {
                conf.append(prefix + ip + suffix + "\n");
            }
        }

        return conf.toString();
    }

    private List<String> findIps(InstanceData instance, boolean ipv6) {
        List<String> ips = Lists.newArrayList();

        for (NetworkAddressData addressInfo : instance.getNetwork().getAddressesList()) {
            InetAddress address = InetAddresses.forString(addressInfo.getIp());
            boolean isIpv6 = address instanceof Inet6Address;
            if (isIpv6 != ipv6) {
                continue;
            }

            ips.add(InetAddresses.toAddrString(address));
        }
        return ips;
    }

    private String buildTunnel(InstanceData instance) {
        StringBuilder conf = new StringBuilder();

        boolean ipv6 = true;
        List<String> ips = findIps(instance, ipv6);

        if (ips.isEmpty()) {
            throw new IllegalStateException();
        }

        String remote = ips.get(0);

        InetAddress localAddress = host.getIpAddress();
        if (!(localAddress instanceof Inet6Address)) {
            throw new IllegalStateException();
        }

        conf.append("ip6ip6 remote " + remote + " local " + InetAddresses.toAddrString(localAddress));

        return conf.toString();
    }

}
