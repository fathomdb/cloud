//package io.fathom.cloud.compute.commands;
//
//import io.fathom.cloud.commands.Cmdlet;
//import io.fathom.cloud.compute.services.NetworkMap;
//import io.fathom.cloud.protobuf.CloudModel.HostData;
//
//import javax.inject.Inject;
//
//import org.kohsuke.args4j.Option;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//public class GetHostConfig extends Cmdlet {
//    private static final Logger log = LoggerFactory.getLogger(GetHostConfig.class);
//
//    @Option(name = "-cidr", usage = "cidr", required = true)
//    public String cidr;
//
//    @Inject
//    NetworkMap networkMap;
//
//    public GetHostConfig() {
//        super("get-host-config");
//    }
//
//    @Override
//    protected void run() throws Exception {
//        HostData host = null;
//
//        for (HostData i : networkMap.listHosts()) {
//            if (cidr.equals(i.getCidr())) {
//                host = i;
//                break;
//            }
//        }
//
//        if (host == null) {
//            throw new IllegalArgumentException("Host not found");
//        }
//
//        // if (!found.hasNetworkDevice()) {
//        // HostData.Builder b = HostData.newBuilder();
//        // b.setNetworkDevice("wlan0");
//        // networkMap.updateHost(found.getId(), b);
//        // }
//
//        println("sudo ip link add virbr0 type bridge");
//        println("sudo ip addr add %s dev virbr0", host.getCidr());
//        println("sudo ip addr add 100.64.0.1/10 dev virbr0");
//
//        // #I think we want to do this if we have an IPv6 router we can
//        // configure:
//        // sudo ip addr add 2601:9:6380:7f:1:1::1/128 dev eth0
//
//        // ip -6 addr add 2601:9:6380:7f:1:1:0:1/128 dev wlan0
//
//        println("sudo ip link set dev virbr0 up");
//
//        println("");
//        println("sudo ip addr add fd00::feed dev lo");
//        println("sudo ip addr add fd00::c10d dev lo");
//
//    }
// }
