package io.fathom.cloud.compute.commands;

import io.fathom.cloud.commands.TypedCmdlet;
import io.fathom.cloud.compute.networks.IpRange;
import io.fathom.cloud.compute.services.NetworkMap;
import io.fathom.cloud.protobuf.CloudModel.HostData;
import io.fathom.cloud.protobuf.CloudModel.HostGroupData;

import javax.inject.Inject;

import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HostCreateCmdlet extends TypedCmdlet {
    private static final Logger log = LoggerFactory.getLogger(HostCreateCmdlet.class);

    @Option(name = "-parent", usage = "parent key", required = true)
    public String parentKey = null;

    @Option(name = "-label", usage = "label", required = false)
    public String label;

    // TODO: We could probably auto-allocate child CIDRs
    @Option(name = "-cidr", usage = "cidr", required = true)
    public String cidr;

    @Option(name = "-net", usage = "network interface", required = false)
    public String networkDevice = "eth0";

    @Inject
    NetworkMap networkMap;

    public HostCreateCmdlet() {
        super("host-create");
    }

    @Override
    protected HostData run0() throws Exception {
        IpRange range = IpRange.parse(cidr);
        if (!range.isIpv6()) {
            throw new IllegalArgumentException("Only IPV6 is supported");
        }

        if (range.getNetmaskLength() > 124) {
            // No real reason, just to keep things sensible
            throw new IllegalArgumentException("Must allocate at least a /124");
        }

        // DatacenterData dc = getDatacenter(datacenterKey);
        //
        // RackData rack;
        // if (rackKey == null) {
        // List<RackData> racks = networkMap.listRacks(dc);
        // if (racks.size() == 0) {
        // throw new
        // IllegalArgumentException("Please create a rack first, with create-rack");
        // } else if (racks.size() == 1) {
        // rack = racks.get(0);
        // } else {
        // throw new
        // IllegalArgumentException("Multiple racks found; please specify the rack to use");
        // }
        // } else {
        // rack = networkMap.findRack(dc, rackKey);
        // if (rack == null) {
        // throw new IllegalArgumentException("Specified rack not found");
        // }
        // }

        HostGroupData parent = networkMap.findHostGroupByKey(parentKey);
        if (parent == null) {
            throw new IllegalArgumentException("Specified parent not found");
        }

        // List<HostData> hosts = networkMap.listHosts(rack);
        // long max = 0;
        // for (HostData host : hosts) {
        // long v = host.getKey();
        // max = Math.max(v, max);
        // }
        //
        // long next = max + 1;
        //
        // if (next < 16) {
        // // We reserve the first 16 for operational stuff
        // next = 16;
        // }
        //
        // if (next >= 65500) {
        // // Reserve some at the end as well
        // // TODO: Reclaim deleted hosts??
        // throw new
        // IllegalArgumentException("Too many machines already configured in rack");
        // }

        // IpRange parentRange = IpRange.parse(parent.getCidr());
        // if (!containsStrict(parentRange, range)) {
        // throw new
        // IllegalArgumentException("Child CIDR must be a sub-range of the parent range");
        // }

        HostData.Builder b = HostData.newBuilder();
        if (label != null) {
            b.setLabel(label);
        }
        b.setHostGroup(parent.getId());
        b.setCidr(cidr);
        b.setNetworkDevice(networkDevice);

        HostData created = networkMap.createHost(b);

        return created;
    }

}
