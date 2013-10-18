package io.fathom.cloud.compute.commands;

//
//import java.util.List;
//
//import org.kohsuke.args4j.Option;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import io.fathom.cloud.networks.IpRange;
//import io.fathom.cloud.protobuf.CloudModel.DatacenterData;
//import io.fathom.cloud.protobuf.CloudModel.RackData;
//
//public class CreateRack extends MapCmdlet {
//    private static final Logger log = LoggerFactory.getLogger(CreateRack.class);
//
//    @Option(name = "-dc", usage = "datacenter", required = false)
//    public Long datacenterKey = null;
//
//    @Option(name = "-label", usage = "label", required = false)
//    public String label;
//
//    @Option(name = "-cidr", usage = "cidr", required = false)
//    public String cidr;
//
//    public CreateRack() {
//        super("create-rack");
//    }
//
//    @Override
//    protected RackData run0() throws Exception {
//        DatacenterData dc = getDatacenter(datacenterKey);
//
//        IpRange range = IpRange.parse(cidr);
//        if (!range.isIpv6()) {
//            throw new IllegalArgumentException("Only IPV6 is supported");
//        }
//
//        int remaining = 128 - range.getNetmaskLength();
//        if (remaining < 64) {
//            throw new IllegalArgumentException("Must allocate at least a /64");
//        }
//
//        List<DatacenterData> dcs = networkMap.listDatacenters();
//        long max = 0;
//        for (DatacenterData dc : dcs) {
//            long v = dc.getKey();
//            max = Math.max(v, max);
//        }
//
//        long next = max + 1;
//        if (next >= 65500) {
//            // Reserve some at the end as well
//            // TODO: Reclaim deleted??
//            throw new IllegalArgumentException("Too many datacenters already configured");
//        }
//
//        RackData.Builder b = RackData.newBuilder();
//        b.setKey(next);
//        b.setLabel(label);
//        b.setCidr(cidr);
//
//        DatacenterData created = networkMap.createDatacenter(b);
//
//        return created;
//    }
// }
