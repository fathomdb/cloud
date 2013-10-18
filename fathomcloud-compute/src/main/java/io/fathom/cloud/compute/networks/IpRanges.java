package io.fathom.cloud.compute.networks;

import java.net.Inet6Address;
import java.net.InetAddress;

public class IpRanges {
    public static boolean isPublic(InetAddress address) {
        if (address instanceof Inet6Address) {
            // Assume public
            return true;
        }

        // TODO: Make static
        IpRange range10 = IpRange.parse("10.0.0.0/8");
        IpRange range192_168 = IpRange.parse("192.168.0.0/16");
        IpRange range172_16 = IpRange.parse("172.17.0.0/12");
        IpRange range100_64 = IpRange.parse("100.64.0.0/10");

        if (range10.contains(address)) {
            return false;
        }

        if (range192_168.contains(address)) {
            return false;
        }

        if (range172_16.contains(address)) {
            return false;
        }

        if (range100_64.contains(address)) {
            return false;
        }

        return true;
    }
}
