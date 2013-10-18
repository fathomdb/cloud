package io.fathom.auto.config;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Hostname {
    /**
     * Return this VM's hostname if possible
     * 
     * @return hostname
     */
    public static String getHostname() {
        String host = "unknown";
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            // ignore
        }
        return host;
    }

}
