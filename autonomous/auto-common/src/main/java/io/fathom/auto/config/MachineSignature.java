package io.fathom.auto.config;

import java.io.File;
import java.io.IOException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;
import com.google.common.io.Files;

public class MachineSignature {

    private static final Logger log = LoggerFactory.getLogger(MachineSignature.class);

    // We try to generate a key that will be the same even if multiple
    // concurrent processes on the machine generate the same key
    public String generateKey() {

        List<String> material = Lists.newArrayList();

        Enumeration<NetworkInterface> networkInterfaces;
        try {
            networkInterfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            throw new IllegalStateException("Error listing network interfaces", e);
        }

        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface networkInterface = networkInterfaces.nextElement();

            try {
                byte[] hardwareAddress = networkInterface.getHardwareAddress();
                if (hardwareAddress != null) {
                    material.add("mac:" + networkInterface.getName() + ":"
                            + BaseEncoding.base16().encode(hardwareAddress));
                } else {
                    log.warn("No hardware address for {}", networkInterface.getName());
                }
            } catch (SocketException e) {
                throw new IllegalStateException("Unable to get hardware address for: " + networkInterface.getName(), e);
            }

            // List<InterfaceAddress> interfaceAddresses = networkInterface
            // .getInterfaceAddresses();
            // for (InterfaceAddress interfaceAddress : interfaceAddresses) {
            // material.add("mac:" + interfaceAddress.)
            // }
        }

        String hostname;
        try {
            hostname = Files.toString(new File("/etc/hostname"), Charsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read /etc/hostname", e);
        }
        hostname = hostname.trim();
        material.add("hostname:" + hostname);

        // TODO: Any more for the mix??

        material.add(computeFileHash("/proc/cpuinfo"));
        Collections.sort(material);

        Hasher hasher = Hashing.md5().newHasher();
        for (String s : material) {
            hasher.putString(s);
            hasher.putByte((byte) 0);
        }

        return hostname + "_" + hasher.hash().toString();
    }

    private String computeFileHash(String path) {
        try {
            byte[] contents = Files.toByteArray(new File(path));
            String hash = Hashing.md5().hashBytes(contents).toString();

            return "filehash:" + path + ":" + hash;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read file " + path, e);
        }
    }
}
