package io.fathom.cloud.compute.commands;

import io.fathom.cloud.compute.networks.IpRange;
import io.fathom.cloud.protobuf.CloudModel.HostData;

import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HostUpdateCmdlet extends NetworkMapCmdlet {
    private static final Logger log = LoggerFactory.getLogger(HostUpdateCmdlet.class);

    @Option(name = "-label", usage = "label", required = false)
    public String label;

    @Option(name = "-cidr", usage = "cidr", required = true)
    public String cidr;

    @Option(name = "-net", usage = "network interface", required = false)
    public String networkDevice;

    public HostUpdateCmdlet() {
        super("host-update");
    }

    @Override
    protected HostData run0() throws Exception {
        IpRange range = IpRange.parse(cidr);
        if (!range.isIpv6()) {
            throw new IllegalArgumentException("Only IPV6 is supported");
        }

        HostData host = networkMap.findHost(cidr);
        if (host == null) {
            throw new IllegalArgumentException("Host with specified CIDR not found");
        }

        HostData.Builder b = HostData.newBuilder(host);
        if (label != null) {
            b.setLabel(label);
        }
        if (networkDevice != null) {
            b.setNetworkDevice(networkDevice);
        }

        HostData created = networkMap.updateHost(host.getId(), b);
        return created;
    }

}
