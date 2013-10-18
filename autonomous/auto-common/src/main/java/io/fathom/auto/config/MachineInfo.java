package io.fathom.auto.config;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

public class MachineInfo {
    public static final MachineInfo INSTANCE = new MachineInfo();

    List<InetAddress> addresses;

    List<InetAddress> getAddresses() {
        // TODO: Don't cache? What if this changes?
        if (this.addresses == null) {
            List<InetAddress> addresses = Lists.newArrayList();

            Enumeration<NetworkInterface> networkInterfaces;
            try {
                networkInterfaces = NetworkInterface.getNetworkInterfaces();
            } catch (SocketException e) {
                throw new IllegalStateException("Error listing network interfaces", e);
            }

            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();

                List<InterfaceAddress> interfaceAddresses = networkInterface.getInterfaceAddresses();
                for (InterfaceAddress interfaceAddress : interfaceAddresses) {
                    InetAddress address = interfaceAddress.getAddress();
                    addresses.add(address);
                }
            }

            this.addresses = addresses;
        }

        return this.addresses;
    }

    public synchronized String getMachineKey() {
        File keyPath = new File("/var/machineid");

        if (!keyPath.exists()) {
            String value = new MachineSignature().generateKey();
            if (!keyPath.exists()) {
                // TODO: Can we do this only-if-not-exists?
                try {
                    Files.write(value.getBytes(Charsets.UTF_8), keyPath);
                } catch (IOException e) {
                    throw new IllegalStateException("Error writing machine key file", e);
                }
            }
        }

        try {
            String machineKey = Files.toString(keyPath, Charsets.UTF_8);
            return machineKey;
        } catch (IOException e) {
            throw new IllegalStateException("Error reading machine key file", e);
        }
    }

    static class CompareInetAddress implements Comparator<InetAddress> {

        @Override
        public int compare(InetAddress l, InetAddress r) {
            int scoreL = score(l);
            int scoreR = score(r);

            return Integer.compare(scoreL, scoreR);
        }
    }

    InetAddress bestIp;

    public InetAddress getIp() {
        if (bestIp == null) {
            List<InetAddress> addresses = getAddresses();

            bestIp = Collections.max(addresses, new CompareInetAddress());
        }

        return bestIp;
    }

    static int score(InetAddress a) {
        if (a.isLoopbackAddress()) {
            return -1000;
        }

        if (a.isLinkLocalAddress()) {
            return -1000;
        }

        if (a.isMulticastAddress()) {
            return -1000;
        }

        byte[] data = a.getAddress();

        if (data.length == 4) {
            // 192.168.0.0/16
            if (data[0] == 192 && data[1] == 168) {
                return -100;
            }

            // 172.16.0.0/20
            if (data[0] == 172 && ((data[1] & 240) == 16)) {
                return -100;
            }

            // 10.0.0.0/24
            if (data[0] == 10) {
                return -50;
            }

            // 100.64.0.0/10
            if (data[0] == 100 && ((data[1] & 192) == 64)) {
                return -20;
            }
        }

        return 0;
    }

}
